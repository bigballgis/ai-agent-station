package com.aiagent.service.llm;

import com.aiagent.config.properties.AiAgentProperties;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain4j OpenAI Provider
 * 支持 OpenAI GPT-4/4o/3.5-turbo 以及所有 OpenAI 兼容 API（如 Azure OpenAI、国内代理）
 *
 * 核心特性:
 * - 使用 langchain4j OpenAiChatModel 替代手动 RestTemplate 调用
 * - 支持多模型实例缓存（按 modelName + temperature 缓存）
 * - 支持流式输出 (OpenAiStreamingChatModel)
 * - 支持 Function Calling / Tool Calling
 * - 支持 JSON Mode (response_format)
 */
@Slf4j
@Component
public class OpenAiLangChain4jProvider implements LangChain4jProvider {

    private final AiAgentProperties.Llm.ProviderConfig openaiConfig;

    public OpenAiLangChain4jProvider(AiAgentProperties aiAgentProperties) {
        this.openaiConfig = aiAgentProperties.getLlm().getOpenai();
    }

    /**
     * 模型实例缓存: key = "modelName|temperature|topP|maxTokens"
     */
    private final Map<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[LangChain4j] OpenAI Provider 初始化 - baseUrl: {}, defaultModel: {}", openaiConfig.getBaseUrl(), openaiConfig.getDefaultModel());
        if (openaiConfig.getApiKey() == null || openaiConfig.getApiKey().isBlank()) {
            log.warn("[LangChain4j] OpenAI API Key 未配置，OpenAI 模型将不可用");
        }
    }

    @PreDestroy
    public void destroy() {
        chatModelCache.clear();
        streamingModelCache.clear();
        log.info("[LangChain4j] OpenAI Provider 已销毁");
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public ChatLanguageModel getChatModel() {
        return getOrCreateChatModel(openaiConfig.getDefaultModel(), 0.7, 0.9, 2048);
    }

    /**
     * 根据配置创建/获取 ChatLanguageModel 实例
     */
    public ChatLanguageModel getOrCreateChatModel(String modelName, double temperature, double topP, int maxTokens) {
        String cacheKey = buildCacheKey(modelName, temperature, topP, maxTokens);
        return chatModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("[LangChain4j] 创建 OpenAI ChatModel: model={}, temp={}, topP={}, maxTokens={}",
                    modelName, temperature, topP, maxTokens);

            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .apiKey(openaiConfig.getApiKey())
                    .baseUrl(openaiConfig.getBaseUrl())
                    .modelName(modelName != null ? modelName : openaiConfig.getDefaultModel())
                    .temperature(temperature)
                    .topP(topP)
                    .maxTokens(maxTokens)
                    .timeout(java.time.Duration.ofSeconds(openaiConfig.getTimeoutSeconds()))
                    .logRequests(openaiConfig.getLogRequests())
                    .logResponses(openaiConfig.getLogResponses());

            return builder.build();
        });
    }

    @Override
    public StreamingChatLanguageModel getStreamingChatModel() {
        String cacheKey = "streaming|" + openaiConfig.getDefaultModel();
        return streamingModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("[LangChain4j] 创建 OpenAI StreamingChatModel: model={}", openaiConfig.getDefaultModel());
            return OpenAiStreamingChatModel.builder()
                    .apiKey(openaiConfig.getApiKey())
                    .baseUrl(openaiConfig.getBaseUrl())
                    .modelName(openaiConfig.getDefaultModel())
                    .temperature(0.7)
                    .timeout(java.time.Duration.ofSeconds(openaiConfig.getTimeoutSeconds()))
                    .logRequests(openaiConfig.getLogRequests())
                    .logResponses(openaiConfig.getLogResponses())
                    .build();
        });
    }

    /**
     * 获取指定配置的流式模型
     */
    public StreamingChatLanguageModel getOrCreateStreamingModel(String modelName, double temperature) {
        String cacheKey = "streaming|" + modelName + "|" + temperature;
        return streamingModelCache.computeIfAbsent(cacheKey, key ->
                OpenAiStreamingChatModel.builder()
                        .apiKey(openaiConfig.getApiKey())
                        .baseUrl(openaiConfig.getBaseUrl())
                        .modelName(modelName != null ? modelName : openaiConfig.getDefaultModel())
                        .temperature(temperature)
                        .timeout(java.time.Duration.ofSeconds(openaiConfig.getTimeoutSeconds()))
                        .logRequests(openaiConfig.getLogRequests())
                        .logResponses(openaiConfig.getLogResponses())
                        .build()
        );
    }

    @Override
    public boolean supportsToolCalling() {
        return true; // GPT-4/4o/3.5-turbo 均支持
    }

    @Override
    public boolean supportsStreaming() {
        return openaiConfig.getApiKey() != null && !openaiConfig.getApiKey().isBlank();
    }

    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4",
                "gpt-3.5-turbo", "o1-preview", "o1-mini"
        );
    }

    /**
     * 使用自定义配置进行对话
     */
    public String chatWithConfig(String systemPrompt, String userMessage, LlmProviderConfig config) {
        ChatLanguageModel model = getOrCreateChatModel(
                config.getModelName(),
                config.getTemperature() != null ? config.getTemperature() : 0.7,
                config.getTopP() != null ? config.getTopP() : 0.9,
                config.getMaxTokens() != null ? config.getMaxTokens() : 2048
        );

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            return model.generate(SystemMessage.from(systemPrompt), UserMessage.from(userMessage))
                    .content().text();
        }
        return model.generate(UserMessage.from(userMessage)).content().text();
    }

    private String buildCacheKey(String modelName, double temperature, double topP, int maxTokens) {
        return String.format("%s|%.2f|%.2f|%d", modelName, temperature, topP, maxTokens);
    }
}
