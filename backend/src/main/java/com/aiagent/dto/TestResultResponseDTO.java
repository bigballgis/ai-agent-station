package com.aiagent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试结果响应 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestResultResponseDTO extends BaseDTO {
    private Long executionId;
    private Long tenantId;
    private Long agentId;
    private Long testCaseId;
    private String actualOutput;
    private String expectedOutput;
    private String status;
    private String comparisonResult;
    private String errorMessage;
}
