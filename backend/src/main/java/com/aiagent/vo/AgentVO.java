package com.aiagent.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentVO extends BaseVO {
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
    private java.time.LocalDateTime publishedAt;
}
