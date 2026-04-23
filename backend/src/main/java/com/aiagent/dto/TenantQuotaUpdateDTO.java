package com.aiagent.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 租户配额更新请求 DTO
 */
@Data
public class TenantQuotaUpdateDTO {

    @Min(value = 0, message = "Agent数量上限不能为负数")
    private Long agentLimit;

    @Min(value = 0, message = "API调用上限不能为负数")
    private Long apiCallLimit;

    @Min(value = 0, message = "Token额度不能为负数")
    private Long tokenLimit;

    @Min(value = 0, message = "存储空间限制不能为负数")
    private Long storageLimit;
}
