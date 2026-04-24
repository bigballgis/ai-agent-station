package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新建议请求 DTO
 */
@Data
@Schema(description = "更新建议请求")
public class UpdateSuggestionRequestDTO {

    @Size(max = 100, message = "{error.validation.title_too_long}")
    @Schema(description = "建议标题", example = "减少响应延迟")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Size(max = 5000, message = "{error.validation.content_too_long}")
    @Schema(description = "建议内容")
    private String content;

    @Schema(description = "优先级(0-10)", example = "5")
    private Integer priority;

    @Size(max = 20, message = "{error.validation.status_too_long}")
    @Schema(description = "状态", example = "pending")
    private String status;

    @Size(max = 20, message = "{error.validation.implementation_status_too_long}")
    @Schema(description = "实现状态", example = "not_started")
    private String implementationStatus;

    @DecimalMin(value = "0.0", message = "{error.validation.impact_min}")
    @Schema(description = "预期影响", example = "0.8")
    private BigDecimal expectedImpact;

    @Schema(description = "实际影响")
    private BigDecimal actualImpact;

    @Schema(description = "实现人ID")
    private Long implementedBy;

    @Schema(description = "更新人ID")
    private Long updatedBy;
}
