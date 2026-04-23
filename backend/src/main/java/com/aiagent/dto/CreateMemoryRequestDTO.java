package com.aiagent.dto;

import lombok.Data;

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
    private String memoryType;

    @NotBlank(message = "记忆内容不能为空")
    private String content;

    private String summary;

    private String tags;

    private Double importance = 0.5;

    private Long tenantId;

    private Long createdBy;
}
