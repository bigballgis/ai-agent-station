package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建API接口请求")
public class ApiInterfaceCreateDTO {

    @NotNull(message = "agentId不能为空")
    @Schema(description = "关联Agent ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;

    @Schema(description = "关联版本ID")
    private Long versionId;

    @NotBlank(message = "path不能为空")
    @Size(max = 500, message = "path不能超过500个字符")
    @Schema(description = "接口路径", requiredMode = Schema.RequiredMode.REQUIRED)
    private String path;

    @NotBlank(message = "method不能为空")
    @Size(max = 10, message = "method不能超过10个字符")
    @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED)
    private String method;

    @Size(max = 1000, message = "接口描述不能超过1000个字符")
    @Schema(description = "接口描述")
    private String description;

    @Schema(description = "是否启用")
    private Boolean isActive = true;

    @Schema(description = "API版本号")
    private String apiVersion = "v1";
}
