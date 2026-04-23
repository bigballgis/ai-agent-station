package com.aiagent.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RoleDTO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
