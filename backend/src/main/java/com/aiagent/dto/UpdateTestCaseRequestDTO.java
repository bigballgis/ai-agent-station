package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 更新测试用例请求 DTO
 */
@Data
@Schema(description = "更新测试用例请求")
public class UpdateTestCaseRequestDTO {

    @Size(max = 100, message = "{error.validation.test_name_too_long}")
    @Schema(description = "测试名称", example = "基本对话测试")
    private String testName;

    @Size(max = 500, message = "{error.validation.description_too_long}")
    @Schema(description = "描述")
    private String description;

    @Size(max = 20, message = "{error.validation.test_type_too_long}")
    @Schema(description = "测试类型", example = "functional")
    private String testType;

    @Schema(description = "输入参数")
    private String inputParams;

    @Schema(description = "预期输出")
    private String expectedOutput;

    @Schema(description = "状态")
    private Integer status;
}
