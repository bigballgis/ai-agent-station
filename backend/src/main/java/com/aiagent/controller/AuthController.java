package com.aiagent.controller;

import com.aiagent.dto.CaptchaResponseDTO;
import com.aiagent.dto.ChangePasswordRequestDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.RegisterRequestDTO;
import com.aiagent.dto.ResetPasswordRequestDTO;
import com.aiagent.dto.UserResponseDTO;
import com.aiagent.entity.User;
import com.aiagent.service.UserService;
import com.aiagent.security.UserPrincipal;
import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.Result;
import com.aiagent.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "认证管理接口")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL_MINUTES = 5;

    /**
     * 获取数学验证码
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取数学验证码")
    public Result<CaptchaResponseDTO> getCaptcha(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        checkRateLimit("captcha:" + clientIp, 10, 60);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int a = random.nextInt(1, 21);
        int b = random.nextInt(1, 21);
        String operator = random.nextBoolean() ? "+" : "-";
        int answer;
        if ("+".equals(operator)) {
            answer = a + b;
        } else {
            // 确保 a >= b，避免负数答案
            if (a < b) {
                int temp = a;
                a = b;
                b = temp;
            }
            answer = a - b;
        }
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String question = a + " " + operator + " " + b + " = ?";
        String redisKey = CAPTCHA_PREFIX + captchaId;
        redisTemplate.opsForValue().set(redisKey, String.valueOf(answer), CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);

        CaptchaResponseDTO dto = new CaptchaResponseDTO(captchaId, question);
        return Result.success(dto);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @OperationLog(value = "用户登录", module = "认证")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        // 如果提供了验证码，先验证
        if (request.getCaptchaId() != null && !request.getCaptchaId().isBlank()) {
            String redisKey = CAPTCHA_PREFIX + request.getCaptchaId();
            String storedAnswer = redisTemplate.opsForValue().get(redisKey);
            // 验证后删除（一次性使用）
            redisTemplate.delete(redisKey);
            if (storedAnswer == null) {
                return Result.fail(400, "验证码已过期，请重新获取");
            }
            if (!storedAnswer.equals(request.getCaptchaAnswer())) {
                return Result.fail(400, "验证码错误");
            }
        }
        return Result.success(authService.login(request.getUsername(), request.getPassword(), request.getTenantId()));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    @OperationLog(value = "用户注册", module = "认证")
    public Result<?> register(@Valid @RequestBody RegisterRequestDTO request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        checkRateLimit("register:" + clientIp, 5, 60);

        return Result.success(authService.register(request));
    }

    /**
     * Token 刷新端点
     * 使用 Refresh Token 获取新的 Access Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    @OperationLog(value = "刷新Token", module = "认证")
    public Result<?> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * 登出（使 Refresh Token 失效并将 Access Token 加入黑名单）
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    @RequiresPermission("auth:manage")
    @OperationLog(value = "用户登出", module = "认证")
    public Result<?> logout(@AuthenticationPrincipal UserPrincipal principal,
                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (principal != null && principal.getId() != null) {
            String accessToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }
            authService.logout(principal.getId(), accessToken);
        }
        return Result.success("登出成功");
    }

    @GetMapping("/userinfo")
    @Operation(summary = "获取当前用户信息")
    public Result<UserResponseDTO> getUserInfo(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return Result.fail(401, "未认证");
        }
        User user = userService.getById(principal.getId());
        return Result.success(DTOConverter.toUserResponseDTO(user));
    }

    /**
     * 修改密码 - 用户自行修改，需验证旧密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    @OperationLog(value = "修改密码", module = "认证")
    public Result<?> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody ChangePasswordRequestDTO request) {
        if (principal == null || principal.getId() == null) {
            return Result.fail(401, "未认证");
        }
        authService.changePassword(principal.getId(), request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功");
    }

    /**
     * 管理员重置密码 - 直接重置指定用户的密码
     */
    @PostMapping("/reset-password")
    @Operation(summary = "管理员重置密码")
    @RequiresPermission("user:manage")
    @OperationLog(value = "管理员重置密码", module = "认证")
    public Result<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request.getUsername(), request.getNewPassword());
        return Result.success("密码重置成功");
    }

    // ==================== Request DTOs ====================

    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度为3-50个字符")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
        private String password;
        private Long tenantId;
        private String captchaId;
        private String captchaAnswer;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public String getCaptchaId() { return captchaId; }
        public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }
        public String getCaptchaAnswer() { return captchaAnswer; }
        public void setCaptchaAnswer(String captchaAnswer) { this.captchaAnswer = captchaAnswer; }
    }

    public static class RefreshRequest {
        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    // ==================== 速率限制辅助方法 ====================

    /**
     * 基于Redis计数器的IP速率限制
     *
     * @param key           Redis key 标识（含IP）
     * @param maxAttempts   时间窗口内最大请求次数
     * @param windowSeconds 时间窗口（秒）
     */
    private void checkRateLimit(String key, int maxAttempts, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
        if (count != null && count > maxAttempts) {
            throw new RuntimeException("请求过于频繁，请稍后重试");
        }
    }

    /**
     * 从HttpServletRequest中获取客户端真实IP
     * 优先读取 X-Forwarded-For header，其次使用 getRemoteAddr()
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个IP，取第一个
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index).trim();
            }
            return ip;
        }
        return request.getRemoteAddr();
    }
}
