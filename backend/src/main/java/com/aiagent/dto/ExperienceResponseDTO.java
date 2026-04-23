package com.aiagent.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 经验响应 DTO
 */
@Data
public class ExperienceResponseDTO implements Serializable {
    private Long id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
