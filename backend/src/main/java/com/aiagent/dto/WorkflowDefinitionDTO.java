package com.aiagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class WorkflowDefinitionDTO {
    @NotBlank(message = "工作流名称不能为空")
    private String name;
    private String description;
    private Map<String, Object> nodes;
    private Map<String, Object> edges;
    private Map<String, Object> triggers;
}
