package com.aiagent.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试用例响应 DTO
 */
@Data
public class TestCaseResponseDTO implements Serializable {
    private Long id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
