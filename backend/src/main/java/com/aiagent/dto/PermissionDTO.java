package com.aiagent.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PermissionDTO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String resourceCode;
    private String actionCode;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
