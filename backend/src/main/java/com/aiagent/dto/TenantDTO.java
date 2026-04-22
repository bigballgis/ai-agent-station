package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Data
public class TenantDTO {
    private Long id;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称不能超过100个字符")
    private String name;

    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String status;
    private String domain;
    private Map<String, Object> quota;
}
