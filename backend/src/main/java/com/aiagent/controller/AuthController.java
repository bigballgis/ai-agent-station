package com.aiagent.controller;

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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "认证管理接口")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @OperationLog(value = "用户登录", module = "认证")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request.getUsername(), request.getPassword(), request.getTenantId()));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    @OperationLog(value = "用户注册", module = "认证")
    public Result<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        return Result.success(authService.register(request));
    }

    /**
     * Token 刷新端点
     * 使用 Refresh Token 获取新的 Access Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    @RequiresPermission("auth:manage")
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

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    }

    public static class RefreshRequest {
        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
}
