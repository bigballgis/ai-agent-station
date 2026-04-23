package com.aiagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeploymentDeployDTO {
    @NotNull(message = "agentId不能为空")
    private Long agentId;
    @NotNull(message = "versionId不能为空")
    private Long versionId;
    private Boolean isCanary = false;
    @Min(value = 0, message = "canaryPercentage不能小于0")
    @Max(value = 100, message = "canaryPercentage不能大于100")
    private Integer canaryPercentage = 100;
    private String remark;
}
