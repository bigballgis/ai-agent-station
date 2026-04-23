package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建建议请求 DTO
 */
@Data
public class CreateSuggestionRequestDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    private Long reflectionId;

    @NotBlank(message = "建议类型不能为空")
    @Size(max = 50, message = "建议类型不能超过50个字符")
    private String suggestionType;

    @NotBlank(message = "建议标题不能为空")
    @Size(max = 100, message = "建议标题不能超过100个字符")
    private String title;

    private String description;

    private String content;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotBlank(message = "状态不能为空")
    @Size(max = 20, message = "状态不能超过20个字符")
    private String status;

    @NotBlank(message = "实现状态不能为空")
    @Size(max = 20, message = "实现状态不能超过20个字符")
    private String implementationStatus;

    private BigDecimal expectedImpact;

    @NotNull(message = "创建人ID不能为空")
    private Long createdBy;
}
