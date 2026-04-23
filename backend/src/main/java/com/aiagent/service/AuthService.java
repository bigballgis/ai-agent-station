package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.User;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.UserRepository;
import com.aiagent.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private final LoginRateLimitService loginRateLimitService;

    public Map<String, Object> login(String username, String password, Long tenantId) {
        // 登录速率限制检查
        String clientIp = getClientIp();
        if (!loginRateLimitService.checkRateLimit(username, clientIp)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), "登录尝试次数过多，请稍后再试");
        }

        User user;
        if (tenantId != null) {
            user = userRepository.findByUsernameAndTenantId(username, tenantId).orElse(null);
        } else {
            user = userRepository.findByUsername(username).orElse(null);
        }

        // 统一错误消息，防止用户名枚举
        if (user == null || !user.getIsActive() || !passwordEncoder.matches(password, user.getPassword())) {
            if (user != null && passwordEncoder.matches(password, user.getPassword()) && !user.getIsActive()) {
                throw new BusinessException(ResultCode.USER_DISABLED);
            }
            loginRateLimitService.recordFailedAttempt(username, clientIp);
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        // 登录成功，重置失败计数
        loginRateLimitService.resetAttempts(username);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getTenantId());

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
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getTenantId());

        // Update refresh token in Redis with new TTL
        redisTemplate.opsForValue().set(redisKey, newRefreshToken, REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        result.put("user", buildUserResponse(user));

        return result;
    }

    /**
     * 登出：清除 Refresh Token 并将 Access Token 加入黑名单
     */
    public void logout(Long userId, String accessToken) {
        // 清除 Refresh Token
        String key = "refresh_token:" + userId;
        redisTemplate.delete(key);

        // 将 Access Token 加入黑名单（TTL = token剩余有效期，最多24小时）
        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            try {
                long remainingSeconds = getTokenRemainingSeconds(accessToken);
                if (remainingSeconds > 0) {
                    String blacklistKey = "token_blacklist:" + accessToken;
                    redisTemplate.opsForValue().set(blacklistKey, "1", remainingSeconds, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.warn("将token加入黑名单失败: {}", e.getMessage());
            }
        }
        log.info("用户登出，已清除 Refresh Token 并加入黑名单: userId={}", userId);
    }

    private long getTokenRemainingSeconds(String token) {
        try {
            var claims = jwtUtil.getClaimsFromToken(token);
            return (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 检查token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String key = "token_blacklist:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
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

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
