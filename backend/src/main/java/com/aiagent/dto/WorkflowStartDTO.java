package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "启动工作流请求")
public class WorkflowStartDTO {

    @NotNull(message = "工作流定义ID不能为空")
    @Schema(description = "工作流定义ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long definitionId;

    @Schema(description = "工作流输入变量")
    private Map<String, Object> variables;
}
