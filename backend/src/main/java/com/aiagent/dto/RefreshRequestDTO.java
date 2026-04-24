package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "刷新令牌请求")
public class RefreshRequestDTO {

    @NotBlank(message = "refreshToken 不能为空")
    @Size(max = 500, message = "refreshToken长度不能超过500个字符")
    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
