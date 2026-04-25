package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "权限数据传输对象")
public class PermissionDTO extends BaseDTO {

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 100, message = "权限名称不能超过100个字符")
    @Schema(description = "权限名称", example = "agent:read")
    private String name;

    @Size(max = 200, message = "权限描述不能超过200个字符")
    @Schema(description = "权限描述", example = "读取Agent权限")
    private String description;

    @NotBlank(message = "资源编码不能为空")
    @Size(max = 50, message = "资源编码不能超过50个字符")
    @Schema(description = "资源编码", example = "agent")
    private String resourceCode;

    @NotBlank(message = "操作编码不能为空")
    @Size(max = 20, message = "操作编码不能超过20个字符")
    @Schema(description = "操作编码", example = "read")
    private String actionCode;
}
