package com.aiagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantVO {
    private Long id;
    private String name;
    private String description;
    private String schemaName;
    private String status;
    private Boolean isActive;
    private Integer maxAgents;
    private Long maxApiCallsPerDay;
    private Long maxTokensPerDay;
    private Long maxMcpCallsPerDay;
    private Long maxStorageMb;
    private Integer usedAgents;
    private Long usedApiCallsToday;
    private Long usedTokensToday;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 不包含 apiKey, apiSecret 等敏感字段
}
