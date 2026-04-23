package com.aiagent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class WorkflowStartDTO {
    @NotNull(message = "工作流定义ID不能为空")
    private Long definitionId;
    private Map<String, Object> variables;
}
