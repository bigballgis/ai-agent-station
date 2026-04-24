package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "角色数据传输对象")
public class RoleDTO implements Serializable {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称", example = "ROLE_ADMIN")
    private String name;

    @Schema(description = "角色描述", example = "管理员角色")
    private String description;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
