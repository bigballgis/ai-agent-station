package com.aiagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentVO {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String type;
    private String category;
    private Boolean isActive;
    private Boolean isTemplate;
    private Integer versionCount;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
