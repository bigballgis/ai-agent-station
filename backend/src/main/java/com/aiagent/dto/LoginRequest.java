package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度为3-50个字符")
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    @Schema(description = "密码", example = "Admin@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "验证码ID")
    private String captchaId;

    @Schema(description = "验证码答案")
    private String captchaAnswer;
}
