package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建记忆请求 DTO
 */
@Data
public class CreateMemoryRequestDTO {

    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    private String sessionId;

    @NotNull(message = "记忆类型不能为空")
    @Size(max = 50, message = "记忆类型不能超过50个字符")
    private String memoryType;

    @NotBlank(message = "记忆内容不能为空")
    @Size(max = 10000, message = "记忆内容不能超过10000个字符")
    private String content;

    @Size(max = 500, message = "摘要不能超过500个字符")
    private String summary;

    @Size(max = 200, message = "标签不能超过200个字符")
    private String tags;

    @DecimalMin(value = "0.0", message = "重要性不能为负数")
    @DecimalMax(value = "1.0", message = "重要性不能超过1")
    private Double importance = 0.5;

    private Long tenantId;

    private Long createdBy;
}
