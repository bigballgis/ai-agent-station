package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新建议请求 DTO
 */
@Data
public class UpdateSuggestionRequestDTO {

    @Size(max = 100, message = "建议标题不能超过100个字符")
    private String title;

    private String description;

    private String content;

    private Integer priority;

    @Size(max = 20, message = "状态不能超过20个字符")
    private String status;

    @Size(max = 20, message = "实现状态不能超过20个字符")
    private String implementationStatus;

    private BigDecimal expectedImpact;

    private BigDecimal actualImpact;

    private Long implementedBy;

    private Long updatedBy;
}
