package com.aiagent.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 建议响应 DTO
 */
@Data
public class SuggestionResponseDTO implements Serializable {
    private Long id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
