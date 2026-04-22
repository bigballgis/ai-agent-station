package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request.getUsername(), request.getPassword(), request.getTenantId()));
    }

    /**
     * Token 刷新端点
     * 使用 Refresh Token 获取新的 Access Token
     */
    @PostMapping("/refresh")
    public Result<?> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * 登出（使 Refresh Token 失效）
     */
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "X-User-ID", required = false) Long userId) {
        if (userId != null) {
            authService.logout(userId);
        }
        return Result.success("登出成功");
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
