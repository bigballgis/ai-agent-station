package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * User response DTO - excludes sensitive fields like password.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户响应信息")
public class UserResponseDTO extends BaseDTO {

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "是否激活", example = "true")
    private Boolean isActive;
    // NOTE: password field intentionally excluded
}
