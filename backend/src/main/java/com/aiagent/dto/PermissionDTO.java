package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "权限数据传输对象")
public class PermissionDTO implements Serializable {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "权限名称", example = "agent:read")
    private String name;

    @Schema(description = "权限描述", example = "读取Agent权限")
    private String description;

    @Schema(description = "资源编码", example = "agent")
    private String resourceCode;

    @Schema(description = "操作编码", example = "read")
    private String actionCode;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
