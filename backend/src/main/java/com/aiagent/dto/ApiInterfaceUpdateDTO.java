package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新API接口请求")
public class ApiInterfaceUpdateDTO {

    @Schema(description = "关联Agent ID")
    private Long agentId;

    @Schema(description = "关联版本ID")
    private Long versionId;

    @Size(max = 500, message = "{error.validation.path_too_long}")
    @Schema(description = "接口路径", example = "/api/v1/chat")
    private String path;

    @Size(max = 10, message = "{error.validation.method_too_long}")
    @Schema(description = "请求方法", example = "POST")
    private String method;

    @Size(max = 1000, message = "{error.validation.description_too_long}")
    @Schema(description = "接口描述")
    private String description;

    @Schema(description = "是否启用", example = "true")
    private Boolean isActive;

    @Schema(description = "是否废弃")
    private Boolean deprecated;

    @Size(max = 500, message = "{error.validation.deprecation_message_too_long}")
    @Schema(description = "废弃说明")
    private String deprecationMessage;
}
