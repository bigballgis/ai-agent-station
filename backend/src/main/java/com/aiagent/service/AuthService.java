package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.User;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.UserRepository;
import com.aiagent.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public Map<String, Object> login(String username, String password, Long tenantId) {
        User user;

        if (tenantId != null) {
            user = userRepository.findByUsernameAndTenantId(username, tenantId)
                    .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        } else {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        }

        if (!user.getIsActive()) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_PASSWORD);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());
        String refreshToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());

        // Store refresh token in Redis with 7-day TTL
        String redisKey = REFRESH_TOKEN_PREFIX + user.getId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("user", buildUserResponse(user));

        return result;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        // Validate the refresh token format and signature
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // Extract user info from the refresh token
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        Long tenantId = jwtUtil.getTenantIdFromToken(refreshToken);

        // Verify the refresh token matches what is stored in Redis
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        String storedRefreshToken = redisTemplate.opsForValue().get(redisKey);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // Verify user still exists and is active
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // Generate new access token and new refresh token
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());
        String newRefreshToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());

        // Update refresh token in Redis with new TTL
        redisTemplate.opsForValue().set(redisKey, newRefreshToken, REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        result.put("user", buildUserResponse(user));

        return result;
    }

    /**
     * 登出：清除 Refresh Token
     */
    public void logout(Long userId) {
        String key = "refresh_token:" + userId;
        redisTemplate.delete(key);
        log.info("用户登出，已清除 Refresh Token: userId={}", userId);
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        userMap.put("tenantId", user.getTenantId());
        return userMap;
    }
}
