package com.aiagent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutionHistoryResponseDTO extends BaseDTO {
    private Long agentId;
    private Long tenantId;
    private Long userId;
    private String message;
    private String role;
    private LocalDateTime timestamp;
}
