package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户数据传输对象")
public class UserDTO extends BaseDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100个字符之间")
    @Schema(description = "密码(至少8位，含大小写字母、数字和特殊字符)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Size(max = 50, message = "手机号不能超过50个字符")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "用户状态", example = "ACTIVE")
    private String status;
}
