package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求 DTO - 用户自行修改密码，需验证旧密码
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequestDTO {

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "新密码长度必须在8-100个字符之间")
    @Schema(description = "新密码(至少8位，含大小写字母、数字和特殊字符)", example = "NewPass@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
