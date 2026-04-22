package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Data
public class AgentDTO {
    private Long id;

    @NotBlank(message = "Agent名称不能为空")
    @Size(max = 100, message = "Agent名称不能超过100个字符")
    private String name;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;

    @Size(max = 100, message = "分类不能超过100个字符")
    private String category;

    @Size(max = 50, message = "类型不能超过50个字符")
    private String type;

    private String graphDefinition;
    private Map<String, Object> config;
    private String status;
    private Boolean isActive;
    private Boolean isTemplate;
}
