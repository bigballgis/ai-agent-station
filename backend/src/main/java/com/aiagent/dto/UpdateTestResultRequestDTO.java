package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 更新测试结果请求 DTO
 */
@Data
@Schema(description = "更新测试结果请求")
public class UpdateTestResultRequestDTO {

    @Schema(description = "实际输出")
    private String actualOutput;

    @Schema(description = "预期输出")
    private String expectedOutput;

    @Size(max = 20, message = "{error.validation.status_too_long}")
    @Schema(description = "状态", example = "passed")
    private String status;

    @Schema(description = "比较结果")
    private String comparisonResult;

    @Schema(description = "错误信息")
    private String errorMessage;
}
