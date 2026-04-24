package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 更新经验请求 DTO
 */
@Data
@Schema(description = "更新经验请求")
public class UpdateExperienceRequestDTO {

    @Size(max = 100, message = "{error.validation.title_too_long}")
    @Schema(description = "经验标题", example = "优化提示词策略")
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

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "更新人ID")
    private Long updatedBy;
}
