package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 更新租户请求 DTO
 */
@Data
@Schema(description = "更新租户请求")
public class UpdateTenantRequestDTO {

    @Size(max = 100, message = "租户名称不能超过100个字符")
    @Schema(description = "租户名称", example = "示例企业")
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    @Schema(description = "租户描述")
    private String description;

    @Size(max = 100, message = "Schema名称不能超过100个字符")
    @Schema(description = "数据库Schema名称")
    private String schemaName;

    @Schema(description = "是否激活", example = "true")
    private Boolean isActive;

    @Schema(description = "最大Agent数量", example = "100")
    private Integer maxAgents;

    @Schema(description = "每日最大API调用次数", example = "10000")
    private Long maxApiCallsPerDay;

    @Schema(description = "每日最大Token数量", example = "1000000")
    private Long maxTokensPerDay;

    @Schema(description = "每日最大MCP调用次数", example = "5000")
    private Long maxMcpCallsPerDay;

    @Schema(description = "最大存储空间(MB)", example = "1024")
    private Long maxStorageMb;
}
