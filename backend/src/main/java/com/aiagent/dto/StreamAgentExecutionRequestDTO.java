package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "SSE流式Agent执行请求")
public class StreamAgentExecutionRequestDTO {

    @NotBlank(message = "message不能为空")
    @Size(max = 10000, message = "消息内容不能超过10000个字符")
    @Schema(description = "用户消息", example = "你好", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "会话ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String sessionId;
}
