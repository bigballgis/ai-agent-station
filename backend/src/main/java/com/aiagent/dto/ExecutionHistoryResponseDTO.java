package com.aiagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExecutionHistoryResponseDTO {
    private Long id;
    private Long agentId;
    private Long tenantId;
    private Long userId;
    private String message;
    private String role;
    private LocalDateTime timestamp;
}
