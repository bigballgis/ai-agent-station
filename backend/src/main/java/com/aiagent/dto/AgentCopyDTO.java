package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Agent复制请求")
public class AgentCopyDTO {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200个字符")
    @Schema(description = "复制后的新名称", example = "客服Agent-副本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newName;
}
