package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色数据传输对象")
public class RoleDTO extends BaseDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称不能超过50个字符")
    @Schema(description = "角色名称", example = "ROLE_ADMIN")
    private String name;

    @Size(max = 200, message = "角色描述不能超过200个字符")
    @Schema(description = "角色描述", example = "管理员角色")
    private String description;
}
