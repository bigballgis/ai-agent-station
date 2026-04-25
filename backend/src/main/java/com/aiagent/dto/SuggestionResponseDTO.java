package com.aiagent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 建议响应 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SuggestionResponseDTO extends BaseDTO {
    private Long tenantId;
    private Long agentId;
    private Long reflectionId;
    private String suggestionType;
    private String title;
    private String description;
    private String content;
    private Integer priority;
    private String status;
    private String implementationStatus;
    private BigDecimal expectedImpact;
    private BigDecimal actualImpact;
    private Long implementedBy;
    private LocalDateTime implementedAt;
    private Long createdBy;
    private Long updatedBy;
}
