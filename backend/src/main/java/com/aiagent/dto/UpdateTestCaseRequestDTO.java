package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 更新测试用例请求 DTO
 */
@Data
public class UpdateTestCaseRequestDTO {

    @Size(max = 100, message = "测试名称不能超过100个字符")
    private String testName;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    @Size(max = 20, message = "测试类型不能超过20个字符")
    private String testType;

    private String inputParams;

    private String expectedOutput;

    private Integer status;
}
