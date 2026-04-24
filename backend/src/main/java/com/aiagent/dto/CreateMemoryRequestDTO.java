package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 创建记忆请求 DTO
 */
@Data
@Schema(description = "创建记忆请求")
public class CreateMemoryRequestDTO {

    @NotNull(message = "{error.validation.agent_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long agentId;

    @Schema(description = "会话ID")
    private String sessionId;

    @NotNull(message = "{error.validation.memory_type_required}")
    @Size(max = 50, message = "{error.validation.memory_type_too_long}")
    @Schema(description = "记忆类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "conversation")
    private String memoryType;

    @NotBlank(message = "{error.validation.content_required}")
    @Size(max = 10000, message = "{error.validation.content_too_long}")
    @Schema(description = "记忆内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Size(max = 500, message = "{error.validation.summary_too_long}")
    @Schema(description = "摘要")
    private String summary;

    @Size(max = 200, message = "{error.validation.tags_too_long}")
    @Schema(description = "标签")
    private String tags;

    @DecimalMin(value = "0.0", message = "{error.validation.importance_min}")
    @DecimalMax(value = "1.0", message = "{error.validation.importance_max}")
    @Schema(description = "重要性(0-1)", example = "0.5")
    private Double importance = 0.5;

    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "租户ID")
    private Long tenantId;

    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "创建人ID")
    private Long createdBy;
}
