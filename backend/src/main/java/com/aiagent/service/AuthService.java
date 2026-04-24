package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.RegisterRequestDTO;
import com.aiagent.entity.User;
import com.aiagent.entity.UserRole;
import com.aiagent.entity.Role;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.UserRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.repository.RoleRepository;
import com.aiagent.security.JwtUtil;
import com.aiagent.security.validator.PasswordPolicyValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimitService loginRateLimitService;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public Map<String, Object> login(String username, String password, Long tenantId) {
        // 登录速率限制检查
        String clientIp = getClientIp();
        String rateLimitMessage = loginRateLimitService.checkRateLimit(username, clientIp);
        if (rateLimitMessage != null) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), rateLimitMessage);
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

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(RegisterRequestDTO request) {
        // 检查密码和确认密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "两次输入的密码不一致");
        }

        // 密码复杂度验证
        List<String> passwordErrors = passwordPolicyValidator.validate(request.getPassword());
        if (!passwordErrors.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), String.join("；", passwordErrors));
        }

        // 检查用户名是否已存在
        if (request.getTenantId() != null) {
            userRepository.findByUsernameAndTenantId(request.getUsername(), request.getTenantId()).ifPresent(u -> {
                throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "该租户下用户名已存在");
            });
        } else {
            userRepository.findByUsername(request.getUsername()).ifPresent(u -> {
                throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "用户名已存在");
            });
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setTenantId(request.getTenantId());
        user.setIsActive(true);
        userRepository.save(user);
        userRepository.flush();

        // 分配默认角色 ROLE_USER
        Role defaultRole;
        if (request.getTenantId() != null) {
            defaultRole = roleRepository.findByNameAndTenantId("ROLE_USER", request.getTenantId())
                    .or(() -> roleRepository.findByName("ROLE_USER"))
                    .orElse(null);
        } else {
            defaultRole = roleRepository.findByName("ROLE_USER").orElse(null);
        }

        if (defaultRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(defaultRole.getId());
            userRoleRepository.save(userRole);
        }

        log.info("用户注册成功: username={}, tenantId={}", request.getUsername(), request.getTenantId());

        // 注册成功后自动登录，返回token（在事务外调用，避免事务传播问题）
        return registerPostProcess(user.getUsername(), request.getPassword(), request.getTenantId());
    }

    /**
     * 注册后处理：在事务外执行登录逻辑，避免事务传播问题
     */
    private Map<String, Object> registerPostProcess(String username, String password, Long tenantId) {
        return login(username, password, tenantId);
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

    /**
     * 用户修改密码 - 验证旧密码后更新
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_PASSWORD);
        }

        // 密码复杂度验证
        List<String> passwordErrors = passwordPolicyValidator.validate(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), String.join("；", passwordErrors));
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 清除该用户的 Refresh Token，强制重新登录
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(redisKey);

        log.info("用户修改密码成功: userId={}", userId);
    }

    /**
     * 管理员重置用户密码 - 直接重置，不需要旧密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 密码复杂度验证
        List<String> passwordErrors = passwordPolicyValidator.validate(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), String.join("；", passwordErrors));
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 清除该用户的 Refresh Token，强制重新登录
        String redisKey = REFRESH_TOKEN_PREFIX + user.getId();
        redisTemplate.delete(redisKey);

        log.info("管理员重置用户密码成功: username={}, operator={}", username, "admin");
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

    private Object buildUserResponse(User user) {
        return DTOConverter.toUserResponseDTO(user);
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
