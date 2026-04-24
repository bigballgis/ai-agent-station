package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码响应")
public class CaptchaResponseDTO {
    @Schema(description = "验证码ID", example = "abc123def456")
    private String captchaId;

    @Schema(description = "验证码问题", example = "15 + 8 = ?")
    private String question;
}
