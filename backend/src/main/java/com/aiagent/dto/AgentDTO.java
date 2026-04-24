package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Data
@Schema(description = "Agent数据传输对象")
public class AgentDTO {

    @Schema(description = "Agent ID")
    private Long id;

    @NotBlank(message = "Agent名称不能为空")
    @Size(max = 100, message = "Agent名称不能超过100个字符")
    @Schema(description = "Agent名称", example = "客服助手", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    @Schema(description = "Agent描述", example = "智能客服助手，支持多轮对话")
    private String description;

    @Size(max = 100, message = "分类不能超过100个字符")
    @Schema(description = "Agent分类", example = "客服")
    private String category;

    @Size(max = 50, message = "类型不能超过50个字符")
    @Schema(description = "Agent类型", example = "CHAT")
    private String type;

    @Schema(description = "图定义(JSON)")
    private String graphDefinition;

    @Schema(description = "Agent配置")
    private Map<String, Object> config;

    @Schema(description = "状态", example = "ACTIVE")
    private String status;

    @Schema(description = "是否激活", example = "true")
    private Boolean isActive;

    @Schema(description = "是否为模板", example = "false")
    private Boolean isTemplate;
}
