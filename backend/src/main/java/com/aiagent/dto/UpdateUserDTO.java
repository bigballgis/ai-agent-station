package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 更新用户 DTO - 仅包含允许更新的非敏感字段（不含password）
 */
@Data
@Schema(description = "更新用户请求")
public class UpdateUserDTO {

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Size(max = 50, message = "手机号不能超过50个字符")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}
