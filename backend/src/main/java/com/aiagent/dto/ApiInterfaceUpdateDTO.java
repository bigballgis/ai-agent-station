package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新API接口请求")
public class ApiInterfaceUpdateDTO {

    @Schema(description = "关联Agent ID")
    private Long agentId;

    @Schema(description = "关联版本ID")
    private Long versionId;

    @Schema(description = "接口路径")
    private String path;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "接口描述")
    private String description;

    @Schema(description = "是否启用")
    private Boolean isActive;

    @Schema(description = "是否废弃")
    private Boolean deprecated;

    @Schema(description = "废弃说明")
    private String deprecationMessage;
}
