package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 更新租户请求 DTO
 */
@Data
public class UpdateTenantRequestDTO {

    @Size(max = 100, message = "租户名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    @Size(max = 100, message = "Schema名称不能超过100个字符")
    private String schemaName;

    private Boolean isActive;

    private Integer maxAgents;

    private Long maxApiCallsPerDay;

    private Long maxTokensPerDay;

    private Long maxMcpCallsPerDay;

    private Long maxStorageMb;
}
