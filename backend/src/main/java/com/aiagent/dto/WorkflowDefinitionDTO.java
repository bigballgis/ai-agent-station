package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "工作流定义请求")
public class WorkflowDefinitionDTO {

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 100, message = "工作流名称不能超过100个字符")
    @Schema(description = "工作流名称", example = "审批流程", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    @Schema(description = "工作流描述", example = "这是一个审批工作流")
    private String description;

    @Schema(description = "工作流节点定义")
    private Map<String, Object> nodes;

    @Schema(description = "工作流边定义")
    private Map<String, Object> edges;

    @Schema(description = "工作流触发器定义")
    private Map<String, Object> triggers;
}
