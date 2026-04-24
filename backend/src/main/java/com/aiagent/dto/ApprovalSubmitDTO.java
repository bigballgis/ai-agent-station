package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "提交审批请求")
public class ApprovalSubmitDTO {

    @NotNull(message = "{error.validation.agent_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "Agent ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;

    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "版本ID", example = "1")
    private Long versionId;

    @Size(max = 500, message = "{error.validation.remark_too_long}")
    @Schema(description = "审批备注", example = "请审核本次变更")
    private String remark;
}
