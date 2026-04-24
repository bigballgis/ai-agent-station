package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 租户配额更新请求 DTO
 */
@Data
@Schema(description = "租户配额更新请求")
public class TenantQuotaUpdateDTO {

    @Min(value = 0, message = "Agent数量上限不能为负数")
    @Schema(description = "Agent数量上限", example = "100")
    private Long agentLimit;

    @Min(value = 0, message = "API调用上限不能为负数")
    @Schema(description = "每日API调用上限", example = "10000")
    private Long apiCallLimit;

    @Min(value = 0, message = "Token额度不能为负数")
    @Schema(description = "每日Token额度上限", example = "1000000")
    private Long tokenLimit;

    @Min(value = 0, message = "存储空间限制不能为负数")
    @Schema(description = "存储空间限制(MB)", example = "1024")
    private Long storageLimit;
}
