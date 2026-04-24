package com.aiagent.dto;

import com.aiagent.annotation.Sensitive;
import com.aiagent.annotation.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求 DTO - 管理员重置用户密码
 */
@Data
@Schema(description = "重置密码请求")
public class ResetPasswordRequestDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "新密码长度必须在8-100个字符之间")
    @Schema(description = "新密码(至少8位，含大小写字母、数字和特殊字符)", example = "NewPass@123", requiredMode = Schema.RequiredMode.REQUIRED)
    @Sensitive(type = SensitiveType.PASSWORD)
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 100, message = "确认密码长度必须在8-100个字符之间")
    @Schema(description = "确认密码", example = "NewPass@123", requiredMode = Schema.RequiredMode.REQUIRED)
    @Sensitive(type = SensitiveType.PASSWORD)
    private String confirmPassword;

    @NotBlank(message = "重置令牌不能为空")
    @Schema(description = "密码重置令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    @Sensitive(type = SensitiveType.PARTIAL, maskPrefix = 4, maskSuffix = 4)
    private String resetToken;
}
