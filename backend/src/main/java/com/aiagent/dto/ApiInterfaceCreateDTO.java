package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建API接口请求")
public class ApiInterfaceCreateDTO {

    @NotNull(message = "{error.validation.agent_id_required}")
    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "关联Agent ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;

    @Positive(message = "{error.validation.id_positive}")
    @Schema(description = "关联版本ID")
    private Long versionId;

    @NotBlank(message = "{error.validation.path_required}")
    @Size(max = 500, message = "{error.validation.path_too_long}")
    @Schema(description = "接口路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "/api/v1/chat")
    private String path;

    @NotBlank(message = "{error.validation.method_required}")
    @Size(max = 10, message = "{error.validation.method_too_long}")
    @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "POST")
    private String method;

    @Size(max = 1000, message = "{error.validation.description_too_long}")
    @Schema(description = "接口描述")
    private String description;

    @Schema(description = "是否启用", example = "true")
    private Boolean isActive = true;

    @Schema(description = "API版本号", example = "v1")
    private String apiVersion = "v1";
}
