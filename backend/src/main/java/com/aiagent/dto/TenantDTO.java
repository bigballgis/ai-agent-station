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

    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Schema(description = "联系人电话")
    private String contactPhone;

    @Schema(description = "租户状态", example = "ACTIVE")
    private String status;

    @Schema(description = "域名")
    private String domain;

    @Schema(description = "配额信息")
    private Map<String, Object> quota;
}
