package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "部署请求")
public class DeploymentDeployDTO {

    @NotNull(message = "agentId不能为空")
    @Schema(description = "Agent ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;

    @NotNull(message = "versionId不能为空")
    @Schema(description = "版本ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long versionId;

    @Schema(description = "是否为金丝雀发布", example = "false")
    private Boolean isCanary = false;

    @Min(value = 0, message = "canaryPercentage不能小于0")
    @Max(value = 100, message = "canaryPercentage不能大于100")
    @Schema(description = "金丝雀发布百分比(0-100)", example = "100")
    private Integer canaryPercentage = 100;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "部署备注", example = "v1.0正式发布")
    private String remark;
}
