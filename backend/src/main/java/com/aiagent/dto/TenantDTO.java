package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Data
@Schema(description = "租户数据传输对象")
public class TenantDTO {

    @Schema(description = "租户ID")
    private Long id;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称不能超过100个字符")
    @Schema(description = "租户名称", example = "示例企业", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Email(message = "联系人邮箱格式不正确")
    @Size(max = 100, message = "联系人邮箱不能超过100个字符")
    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Size(max = 20, message = "联系人电话不能超过20个字符")
    @Schema(description = "联系人电话")
    private String contactPhone;

    @Size(max = 50, message = "租户状态不能超过50个字符")
    @Schema(description = "租户状态", example = "ACTIVE")
    private String status;

    @Size(max = 100, message = "域名不能超过100个字符")
    @Schema(description = "域名")
    private String domain;

    @Schema(description = "配额信息")
    private Map<String, Object> quota;
}
