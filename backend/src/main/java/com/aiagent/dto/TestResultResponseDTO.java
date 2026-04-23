package com.aiagent.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试结果响应 DTO
 */
@Data
public class TestResultResponseDTO implements Serializable {
    private Long id;
    private Long executionId;
    private Long tenantId;
    private Long agentId;
    private Long testCaseId;
    private String actualOutput;
    private String expectedOutput;
    private String status;
    private String comparisonResult;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
