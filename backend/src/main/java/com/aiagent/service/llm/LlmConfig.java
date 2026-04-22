package com.aiagent.service.llm;

import lombok.Data;

@Data
public class LlmConfig {
    private String model;
    private Double temperature = 0.7;
    private Double topP = 0.9;
    private Integer maxTokens = 2048;
    private Integer contextWindow = 4096;
    private Double frequencyPenalty = 0.0;
    private Double presencePenalty = 0.0;
    private Integer timeoutSeconds = 30;
}
