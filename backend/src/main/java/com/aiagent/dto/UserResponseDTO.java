package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User response DTO - excludes sensitive fields like password.
 */
@Data
@Schema(description = "用户响应信息")
public class UserResponseDTO implements Serializable {
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "是否激活", example = "true")
    private Boolean isActive;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    // NOTE: password field intentionally excluded
}
