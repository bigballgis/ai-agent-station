package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建测试用例请求 DTO
 */
@Data
public class CreateTestCaseRequestDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    @NotBlank(message = "测试名称不能为空")
    @Size(max = 100, message = "测试名称不能超过100个字符")
    private String testName;

    @NotBlank(message = "测试编码不能为空")
    @Size(max = 50, message = "测试编码不能超过50个字符")
    private String testCode;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    @NotBlank(message = "测试类型不能为空")
    @Size(max = 20, message = "测试类型不能超过20个字符")
    private String testType;

    @NotNull(message = "输入参数不能为空")
    private String inputParams;

    @NotNull(message = "预期输出不能为空")
    private String expectedOutput;

    private Integer status;

    @NotNull(message = "创建人ID不能为空")
    private Long createdBy;
}
