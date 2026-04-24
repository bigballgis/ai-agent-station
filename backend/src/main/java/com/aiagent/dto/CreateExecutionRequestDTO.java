package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 创建测试执行请求 DTO
 */
@Data
@Schema(description = "创建测试执行请求")
public class CreateExecutionRequestDTO {

    @NotNull(message = "{error.validation.tenant_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @NotNull(message = "{error.validation.agent_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long agentId;

    @NotNull(message = "{error.validation.test_case_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "测试用例ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long testCaseId;

    @NotBlank(message = "{error.validation.execution_type_required}")
    @Size(max = 20, message = "{error.validation.execution_type_too_long}")
    @Schema(description = "执行类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "manual")
    private String executionType;

    @NotNull(message = "{error.validation.executor_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "执行人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long executorId;
}
