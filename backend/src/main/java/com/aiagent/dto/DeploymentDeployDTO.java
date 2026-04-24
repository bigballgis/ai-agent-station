package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "部署请求")
public class DeploymentDeployDTO {

    @NotNull(message = "{error.validation.agent_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "Agent ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;

    @NotNull(message = "{error.validation.version_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "版本ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long versionId;

    @Schema(description = "是否为金丝雀发布", example = "false")
    private Boolean isCanary = false;

    @Min(value = 0, message = "{error.validation.canary_percentage_min}")
    @Max(value = 100, message = "{error.validation.canary_percentage_max}")
    @Schema(description = "金丝雀发布百分比(0-100)", example = "100")
    private Integer canaryPercentage = 100;

    @Size(max = 500, message = "{error.validation.remark_too_long}")
    @Schema(description = "部署备注", example = "v1.0正式发布")
    private String remark;
}
