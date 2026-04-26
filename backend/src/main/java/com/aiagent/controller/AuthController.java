package com.aiagent.controller;

import com.aiagent.dto.CaptchaResponseDTO;
import com.aiagent.dto.ChangePasswordRequestDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.LoginRequest;
import com.aiagent.dto.RefreshRequestDTO;
import com.aiagent.dto.RegisterRequestDTO;
import com.aiagent.dto.ResetPasswordRequestDTO;
import com.aiagent.dto.UserResponseDTO;
import com.aiagent.entity.User;
import com.aiagent.service.UserService;
import com.aiagent.security.UserPrincipal;
import com.aiagent.annotation.Audited;
import com.aiagent.annotation.AuditAction;
import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.Result;
import com.aiagent.exception.RateLimitException;
import com.aiagent.service.AuthService;
import com.aiagent.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
        String clientIp = SecurityUtils.getClientIp(request);
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
    @Operation(summary = "用户登录", description = "使用用户名和密码登录系统，返回JWT令牌")
    @OperationLog(value = "用户登录", module = "认证")
    @Audited(action = AuditAction.LOGIN, module = "认证", description = "用户登录", resourceType = "User")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败或验证码错误"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "403", description = "用户已被禁用"),
            @ApiResponse(responseCode = "429", description = "登录尝试过于频繁")
    })
    public Result<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // 登录接口限流：防止暴力破解
        String clientIp = SecurityUtils.getClientIp(httpRequest);
        checkRateLimit("login:" + clientIp, 10, 300);

        // 如果提供了验证码，先验证
        if (request.getCaptchaId() != null && !request.getCaptchaId().isBlank()) {
            String redisKey = CAPTCHA_PREFIX + request.getCaptchaId();
            String storedAnswer = redisTemplate.opsForValue().get(redisKey);
            // 验证后删除（一次性使用）
            redisTemplate.delete(redisKey);
            if (storedAnswer == null) {
                return Result.error(400, "验证码已过期，请重新获取");
            }
            if (!storedAnswer.equals(request.getCaptchaAnswer())) {
                return Result.error(400, "验证码错误");
            }
        }
        return Result.success(authService.login(request.getUsername(), request.getPassword(), request.getTenantId()));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @OperationLog(value = "用户注册", module = "认证")
    @Audited(action = AuditAction.CREATE, module = "认证", description = "用户注册", resourceType = "User")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "409", description = "用户名已存在")
    })
    public Result<?> register(@Valid @RequestBody RegisterRequestDTO request, HttpServletRequest httpRequest) {
        String clientIp = SecurityUtils.getClientIp(httpRequest);
        checkRateLimit("register:" + clientIp, 5, 60);

        return Result.success(authService.register(request));
    }

    /**
     * Token 刷新端点
     * 使用 Refresh Token 获取新的 Access Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用Refresh Token获取新的Access Token")
    @OperationLog(value = "刷新Token", module = "认证")
    @Audited(action = AuditAction.LOGIN, module = "认证", description = "刷新Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "401", description = "Refresh Token无效或已过期")
    })
    public Result<?> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        return Result.success(authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * 登出（使 Refresh Token 失效并将 Access Token 加入黑名单）
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    @RequiresPermission("auth:manage")
    @OperationLog(value = "用户登出", module = "认证")
    @Audited(action = AuditAction.LOGOUT, module = "认证", description = "用户登出")
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
            return Result.error(401, "未认证");
        }
        User user = userService.getUserById(principal.getId());
        return Result.success(DTOConverter.toUserResponseDTO(user));
    }

    /**
     * 修改密码 - 用户自行修改，需验证旧密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "用户自行修改密码，需验证旧密码")
    @OperationLog(value = "修改密码", module = "认证")
    @Audited(action = AuditAction.PASSWORD_CHANGE, module = "认证", description = "修改密码")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "密码修改成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败或旧密码错误"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<?> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody ChangePasswordRequestDTO request) {
        if (principal == null || principal.getId() == null) {
            return Result.error(401, "未认证");
        }
        authService.changePassword(principal.getId(), request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功");
    }

    /**
     * 管理员重置密码 - 直接重置指定用户的密码
     */
    @PostMapping("/reset-password")
    @Operation(summary = "管理员重置密码", description = "管理员直接重置指定用户的密码")
    @RequiresPermission("user:manage")
    @OperationLog(value = "管理员重置密码", module = "认证")
    @Audited(action = AuditAction.PASSWORD_CHANGE, module = "认证", description = "管理员重置密码", resourceType = "User")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "密码重置成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "429", description = "重置请求过于频繁")
    })
    public Result<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request,
                                   HttpServletRequest httpRequest) {
        // 密码重置接口限流
        String clientIp = SecurityUtils.getClientIp(httpRequest);
        checkRateLimit("reset-password:" + clientIp, 5, 300);

        authService.resetPassword(request.getUsername(), request.getNewPassword());
        return Result.success("密码重置成功");
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
            throw new RateLimitException("请求过于频繁，请稍后重试");
        }
    }
}
