package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "取消工作流请求")
public class WorkflowCancelDTO {

    @Size(max = 500, message = "取消原因不能超过500个字符")
    @Schema(description = "取消原因", example = "业务需求变更，取消本次工作流")
    private String reason;
}
