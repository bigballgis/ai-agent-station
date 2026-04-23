package com.aiagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequestDTO {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
