package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建租户请求 DTO
 */
@Data
public class CreateTenantRequestDTO {

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    @Size(max = 100, message = "Schema名称不能超过100个字符")
    private String schemaName;

    private Boolean isActive = true;

    private Integer maxAgents = 100;

    private Long maxApiCallsPerDay = 10000L;

    private Long maxTokensPerDay = 1000000L;

    private Long maxMcpCallsPerDay = 5000L;

    private Long maxStorageMb = 1024L;
}
