package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.RegisterRequestDTO;
import com.aiagent.entity.PasswordHistory;
import com.aiagent.entity.User;
import com.aiagent.entity.UserRole;
import com.aiagent.entity.Role;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.UserRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.repository.RoleRepository;
import com.aiagent.repository.PasswordHistoryRepository;
import com.aiagent.security.JwtUtil;
import com.aiagent.security.validator.PasswordPolicyValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    @Value("${ai-agent.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${ai-agent.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${ai-agent.security.password-history-count:5}")
    private int passwordHistoryCount;

    @Value("${ai-agent.security.refresh-token-ttl-days:7}")
    private long refreshTokenTtlDays;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimitService loginRateLimitService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final SessionService sessionService;

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

        // 用户不存在或已禁用
        if (user == null || !user.getIsActive()) {
            if (user != null && !user.getIsActive()) {
                throw new BusinessException(ResultCode.USER_DISABLED);
            }
            loginRateLimitService.recordFailedAttempt(username, clientIp);
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        // 检查账户是否被锁定
        checkAccountLockout(user);

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 记录失败尝试并检查是否需要锁定
            recordFailedLoginAttempt(user);
            loginRateLimitService.recordFailedAttempt(username, clientIp);
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        // 登录成功，重置失败计数
        loginRateLimitService.resetAttempts(username);
        resetFailedLoginAttempts(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getTenantId());

        // Store refresh token in Redis with 7-day TTL
        String redisKey = REFRESH_TOKEN_PREFIX + user.getId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, refreshTokenTtlDays, TimeUnit.DAYS);

        // Create session record and enforce concurrent session limit
        try {
            String sessionId = java.util.UUID.nameUUIDFromBytes(token.getBytes()).toString();
            sessionService.createSession(user.getId(), user.getUsername(), sessionId);
        } catch (Exception e) {
            log.warn("创建会话记录失败（不影响登录）: {}", e.getMessage());
        }

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

        // 保存密码历史
        savePasswordHistory(user.getId(), user.getPassword());

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
        redisTemplate.opsForValue().set(redisKey, newRefreshToken, refreshTokenTtlDays, TimeUnit.DAYS);

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

        // 检查密码历史，防止重用最近5个密码
        checkPasswordHistory(userId, newPassword);

        // 更新密码
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        // 保存密码历史
        savePasswordHistory(userId, encodedNewPassword);

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

        // 检查密码历史，防止重用最近5个密码
        checkPasswordHistory(user.getId(), newPassword);

        // 更新密码
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        // 保存密码历史
        savePasswordHistory(user.getId(), encodedNewPassword);

        // 清除该用户的 Refresh Token，强制重新登录
        String redisKey = REFRESH_TOKEN_PREFIX + user.getId();
        redisTemplate.delete(redisKey);

        log.info("管理员重置用户密码成功: username={}, operator={}", username, "admin");
    }

    /**
     * 检查账户是否被锁定，如果锁定中则抛出异常
     */
    private void checkAccountLockout(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            log.warn("账户已锁定: username={}, lockedUntil={}", user.getUsername(), user.getLockedUntil());
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    String.format("账户已被锁定，请%d分钟后再试", remainingMinutes));
        }
        // 锁定已过期，自动解锁
        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now())) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    /**
     * 记录一次登录失败，达到阈值后锁定账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordFailedLoginAttempt(User user) {
        int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            log.warn("账户已锁定: username={}, attempts={}, lockedUntil={}",
                    user.getUsername(), attempts, user.getLockedUntil());
        }

        userRepository.save(user);
    }

    /**
     * 登录成功后重置失败计数
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetFailedLoginAttempts(User user) {
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    /**
     * 检查密码是否在历史记录中（防止重用最近N个密码）
     */
    private void checkPasswordHistory(Long userId, String newPassword) {
        List<PasswordHistory> history = passwordHistoryRepository.findTopNByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, passwordHistoryCount));
        for (PasswordHistory ph : history) {
            if (passwordEncoder.matches(newPassword, ph.getPasswordHash())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "不能使用最近" + passwordHistoryCount + "次使用过的密码");
            }
        }
    }

    /**
     * 保存密码到历史记录
     */
    private void savePasswordHistory(Long userId, String encodedPassword) {
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(encodedPassword);
        passwordHistoryRepository.save(history);
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
        HttpServletRequest request = getCurrentHttpServletRequest();
        if (request == null) {
            return "unknown";
        }
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

    private HttpServletRequest getCurrentHttpServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
