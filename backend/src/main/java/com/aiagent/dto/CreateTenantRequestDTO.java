package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建租户请求 DTO
 */
@Data
@Schema(description = "创建租户请求")
public class CreateTenantRequestDTO {

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称不能超过100个字符")
    @Schema(description = "租户名称", example = "示例企业", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    @Schema(description = "租户描述", example = "示例企业租户")
    private String description;

    @Size(max = 100, message = "Schema名称不能超过100个字符")
    @Schema(description = "数据库Schema名称")
    private String schemaName;

    @Schema(description = "是否激活", example = "true")
    private Boolean isActive = true;

    @Min(value = 1, message = "最大Agent数量不能小于1")
    @Schema(description = "最大Agent数量", example = "100")
    private Integer maxAgents = 100;

    @Min(value = 0, message = "每日最大API调用次数不能为负数")
    @Schema(description = "每日最大API调用次数", example = "10000")
    private Long maxApiCallsPerDay = 10000L;

    @Min(value = 0, message = "每日最大Token数量不能为负数")
    @Schema(description = "每日最大Token数量", example = "1000000")
    private Long maxTokensPerDay = 1000000L;

    @Min(value = 0, message = "每日最大MCP调用次数不能为负数")
    @Schema(description = "每日最大MCP调用次数", example = "5000")
    private Long maxMcpCallsPerDay = 5000L;

    @Min(value = 0, message = "最大存储空间不能为负数")
    @Schema(description = "最大存储空间(MB)", example = "1024")
    private Long maxStorageMb = 1024L;
}
