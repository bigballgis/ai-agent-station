package com.aiagent.dto;

import com.aiagent.annotation.Sensitive;
import com.aiagent.annotation.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO
 */
@Data
@Schema(description = "用户注册请求")
public class RegisterRequestDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Schema(description = "用户名", example = "newuser", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100个字符之间")
    @Schema(description = "密码(至少8位，含大小写字母、数字和特殊字符)", example = "Admin@12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @Sensitive(type = SensitiveType.PASSWORD)
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 100, message = "确认密码长度必须在8-100个字符之间")
    @Schema(description = "确认密码", example = "Admin@12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @Sensitive(type = SensitiveType.PASSWORD)
    private String confirmPassword;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;
}
