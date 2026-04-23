package com.aiagent.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 记忆响应 DTO
 */
@Data
public class MemoryResponseDTO implements Serializable {
    private Long id;
    private Long agentId;
    private String sessionId;
    private String memoryType;
    private String content;
    private String summary;
    private String tags;
    private Double importance;
    private Integer accessCount;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    private Long tenantId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
