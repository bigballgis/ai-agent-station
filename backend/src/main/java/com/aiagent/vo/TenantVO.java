package com.aiagent.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TenantVO extends BaseVO {
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
    // 不包含 apiKey, apiSecret 等敏感字段
}
