package com.aiagent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalSubmitDTO {
    @NotNull(message = "agentId不能为空")
    private Long agentId;
    private Long versionId;
    private String remark;
}
