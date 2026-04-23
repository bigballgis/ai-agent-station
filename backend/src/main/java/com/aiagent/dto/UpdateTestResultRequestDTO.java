package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 更新测试结果请求 DTO
 */
@Data
public class UpdateTestResultRequestDTO {

    private String actualOutput;

    private String expectedOutput;

    @Size(max = 20, message = "状态不能超过20个字符")
    private String status;

    private String comparisonResult;

    private String errorMessage;
}
