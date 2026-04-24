package com.aiagent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class AgentVO {
    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private String status;
    private String category;
    private Map<String, Object> config;
    private String icon;
    private String language;
    private List<String> tags;
    private Boolean isActive;
    private Boolean isTemplate;
    private Double rating;
    private Integer usageCount;
    private Long version;
    private Long publishedVersionId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
