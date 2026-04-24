package com.aiagent.dto;

import com.aiagent.annotation.Sensitive;
import com.aiagent.annotation.SensitiveType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @Sensitive(type = SensitiveType.PARTIAL, maskPrefix = 4, maskSuffix = 4)
    @JsonSerialize(using = com.aiagent.serializer.SensitiveDataSerializer.class)
    private String refreshToken;
}
