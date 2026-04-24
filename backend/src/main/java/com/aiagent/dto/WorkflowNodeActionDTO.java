package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "工作流节点操作请求")
public class WorkflowNodeActionDTO {

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "节点操作备注", example = "已审核通过")
    private String comment;
}
