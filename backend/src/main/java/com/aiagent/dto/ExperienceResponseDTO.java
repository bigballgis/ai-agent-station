package com.aiagent.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 经验响应 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExperienceResponseDTO extends BaseDTO {
    private Long tenantId;
    private Long agentId;
    private String experienceType;
    private String experienceCode;
    private String title;
    private String description;
    private String content;
    private List<String> tags;
    private Integer usageCount;
    private BigDecimal effectivenessScore;
    private Integer status;
    private Long createdBy;
    private Long updatedBy;
}
