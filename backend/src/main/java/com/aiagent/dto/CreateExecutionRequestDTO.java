package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建测试执行请求 DTO
 */
@Data
public class CreateExecutionRequestDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    @NotNull(message = "测试用例ID不能为空")
    private Long testCaseId;

    @NotBlank(message = "执行类型不能为空")
    @Size(max = 20, message = "执行类型不能超过20个字符")
    private String executionType;

    @NotNull(message = "执行人ID不能为空")
    private Long executorId;
}
