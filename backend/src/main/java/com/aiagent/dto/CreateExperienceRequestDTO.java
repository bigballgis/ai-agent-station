package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建经验请求 DTO
 */
@Data
@Schema(description = "创建经验请求")
public class CreateExperienceRequestDTO {

    @NotNull(message = "{error.validation.tenant_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @NotNull(message = "{error.validation.agent_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long agentId;

    @NotBlank(message = "{error.validation.experience_type_required}")
    @Size(max = 50, message = "{error.validation.experience_type_too_long}")
    @Schema(description = "经验类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "optimization")
    private String experienceType;

    @NotBlank(message = "{error.validation.experience_code_required}")
    @Size(max = 50, message = "{error.validation.experience_code_too_long}")
    @Schema(description = "经验编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String experienceCode;

    @NotBlank(message = "{error.validation.title_required}")
    @Size(max = 100, message = "{error.validation.title_too_long}")
    @Schema(description = "经验标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "优化提示词策略")
    private String title;

    @Size(max = 2000, message = "{error.validation.description_too_long}")
    @Schema(description = "描述")
    private String description;

    @Size(max = 10000, message = "{error.validation.content_too_long}")
    @Schema(description = "经验内容")
    private String content;

    @Schema(description = "标签列表")
    private List<String> tags;

    @DecimalMin(value = "0.0", message = "{error.validation.score_min}")
    @DecimalMax(value = "10.0", message = "{error.validation.effectiveness_score_max}")
    @Schema(description = "有效性评分(0-10)", example = "8.5")
    private BigDecimal effectivenessScore;

    @NotNull(message = "{error.validation.status_required}")
    @Min(value = 0, message = "{error.validation.status_min}")
    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @NotNull(message = "{error.validation.creator_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "创建人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createdBy;
}
