package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "提交审批请求")
public class ApprovalSubmitDTO {

    @NotNull(message = "agentId不能为空")
    @Schema(description = "Agent ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;

    @Schema(description = "版本ID", example = "1")
    private Long versionId;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "审批备注", example = "请审核本次变更")
    private String remark;
}
