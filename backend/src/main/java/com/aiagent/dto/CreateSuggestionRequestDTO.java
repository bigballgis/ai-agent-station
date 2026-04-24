package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建建议请求 DTO
 */
@Data
@Schema(description = "创建建议请求")
public class CreateSuggestionRequestDTO {

    @NotNull(message = "{error.validation.tenant_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @NotNull(message = "{error.validation.agent_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long agentId;

    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "反思ID")
    private Long reflectionId;

    @NotBlank(message = "{error.validation.suggestion_type_required}")
    @Size(max = 50, message = "{error.validation.suggestion_type_too_long}")
    @Schema(description = "建议类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "performance")
    private String suggestionType;

    @NotBlank(message = "{error.validation.title_required}")
    @Size(max = 100, message = "{error.validation.title_too_long}")
    @Schema(description = "建议标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "减少响应延迟")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Size(max = 5000, message = "{error.validation.content_too_long}")
    @Schema(description = "建议内容")
    private String content;

    @NotNull(message = "{error.validation.priority_required}")
    @Min(value = 0, message = "{error.validation.priority_min}")
    @Max(value = 10, message = "{error.validation.priority_max}")
    @Schema(description = "优先级(0-10)", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer priority;

    @NotBlank(message = "{error.validation.status_required}")
    @Size(max = 20, message = "{error.validation.status_too_long}")
    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "pending")
    private String status;

    @NotBlank(message = "{error.validation.implementation_status_required}")
    @Size(max = 20, message = "{error.validation.implementation_status_too_long}")
    @Schema(description = "实现状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "not_started")
    private String implementationStatus;

    @DecimalMin(value = "0.0", message = "{error.validation.impact_min}")
    @Schema(description = "预期影响", example = "0.8")
    private BigDecimal expectedImpact;

    @NotNull(message = "{error.validation.creator_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "创建人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createdBy;
}
