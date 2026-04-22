package com.aiagent.service.llm;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * LangChain4j 原生 LLM 配置
 * 基于 langchain4j ChatLanguageModel 构建参数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LlmProviderConfig {

    /**
     * 提供商标识: openai, qwen, ollama, azure-openai
     */
    private String provider;

    /**
     * 模型名称: gpt-4, qwen-plus, llama3, etc.
     */
    private String modelName;

    /**
     * API Key（从环境变量注入，不硬编码）
     */
    private String apiKey;

    /**
     * API Base URL（支持自定义端点，如私有化部署）
     */
    private String baseUrl;

    /**
     * 温度参数 0.0-2.0
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * Top-P 采样参数 0.0-1.0
     */
    @Builder.Default
    private Double topP = 0.9;

    /**
     * 最大生成 Token 数
     */
    @Builder.Default
    private Integer maxTokens = 2048;

    /**
     * 上下文窗口大小
     */
    @Builder.Default
    private Integer contextWindow = 4096;

    /**
     * 频率惩罚 -2.0-2.0
     */
    @Builder.Default
    private Double frequencyPenalty = 0.0;

    /**
     * 存在惩罚 -2.0-2.0
     */
    @Builder.Default
    private Double presencePenalty = 0.0;

    /**
     * 超时时间（秒）
     */
    @Builder.Default
    private Integer timeoutSeconds = 60;

    /**
     * 是否启用流式输出
     */
    @Builder.Default
    private Boolean streamEnabled = false;

    /**
     * 是否启用日志记录
     */
    @Builder.Default
    private Boolean logRequests = true;

    /**
     * 是否启用响应日志
     */
    @Builder.Default
    private Boolean logResponses = false;

    /**
     * 随机种子（可复现输出）
     */
    private Long seed;

    /**
     * 停止序列
     */
    private java.util.List<String> stopSequences;

    /**
     * 响应格式: text / json_object
     */
    private String responseFormat;

    /**
     * 向后兼容：从旧版 LlmConfig 转换
     */
    public static LlmProviderConfig fromLegacy(LlmConfig legacy, String providerName) {
        return LlmProviderConfig.builder()
                .provider(providerName)
                .modelName(legacy.getModel())
                .temperature(legacy.getTemperature())
                .topP(legacy.getTopP())
                .maxTokens(legacy.getMaxTokens())
                .contextWindow(legacy.getContextWindow())
                .frequencyPenalty(legacy.getFrequencyPenalty())
                .presencePenalty(legacy.getPresencePenalty())
                .timeoutSeconds(legacy.getTimeoutSeconds())
                .build();
    }
}
