package com.aiagent.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User response DTO - excludes sensitive fields like password.
 */
@Data
public class UserResponseDTO implements Serializable {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private Boolean isActive;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // NOTE: password field intentionally excluded
}
