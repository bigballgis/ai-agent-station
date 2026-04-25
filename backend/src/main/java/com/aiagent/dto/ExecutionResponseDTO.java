package com.aiagent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 测试执行响应 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutionResponseDTO extends BaseDTO {
    private Long tenantId;
    private Long agentId;
    private Long testCaseId;
    private String executionType;
    private Long executorId;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer executionTime;
    private String errorMessage;
}
