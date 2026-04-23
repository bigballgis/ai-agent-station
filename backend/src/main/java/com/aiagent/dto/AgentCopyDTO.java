package com.aiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentCopyDTO {
    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200个字符")
    private String newName;
}
