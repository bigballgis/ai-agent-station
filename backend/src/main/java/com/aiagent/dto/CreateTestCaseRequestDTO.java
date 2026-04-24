package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 创建测试用例请求 DTO
 */
@Data
@Schema(description = "创建测试用例请求")
public class CreateTestCaseRequestDTO {

    @NotNull(message = "{error.validation.tenant_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @NotNull(message = "{error.validation.agent_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long agentId;

    @NotBlank(message = "{error.validation.test_name_required}")
    @Size(max = 100, message = "{error.validation.test_name_too_long}")
    @Schema(description = "测试名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "基本对话测试")
    private String testName;

    @NotBlank(message = "{error.validation.test_code_required}")
    @Size(max = 50, message = "{error.validation.test_code_too_long}")
    @Schema(description = "测试编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TC001")
    private String testCode;

    @Size(max = 500, message = "{error.validation.description_too_long}")
    @Schema(description = "描述")
    private String description;

    @NotBlank(message = "{error.validation.test_type_required}")
    @Size(max = 20, message = "{error.validation.test_type_too_long}")
    @Schema(description = "测试类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "functional")
    private String testType;

    @NotNull(message = "{error.validation.input_params_required}")
    @Schema(description = "输入参数", requiredMode = Schema.RequiredMode.REQUIRED)
    private String inputParams;

    @NotNull(message = "{error.validation.expected_output_required}")
    @Schema(description = "预期输出", requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedOutput;

    @Min(value = 0, message = "{error.validation.status_min}")
    @Schema(description = "状态")
    private Integer status;

    @NotNull(message = "{error.validation.creator_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "创建人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createdBy;
}
