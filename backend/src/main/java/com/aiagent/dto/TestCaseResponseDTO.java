package com.aiagent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 测试用例响应 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestCaseResponseDTO extends BaseDTO {
    private Long tenantId;
    private Long agentId;
    private String testName;
    private String testCode;
    private String description;
    private String testType;
    private String inputParams;
    private String expectedOutput;
    private Integer status;
    private Long createdBy;
}
