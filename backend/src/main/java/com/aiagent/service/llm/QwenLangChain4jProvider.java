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
 * LangChain4j 通义千问 (Qwen) Provider
 * 基于 DashScope OpenAI 兼容模式，使用 langchain4j OpenAiChatModel
 *
 * 支持模型: qwen-turbo, qwen-plus, qwen-max, qwen-long
 */
@Slf4j
@Component
public class QwenLangChain4jProvider implements LangChain4jProvider {

    private final AiAgentProperties.Llm.ProviderConfig qwenConfig;

    public QwenLangChain4jProvider(AiAgentProperties aiAgentProperties) {
        this.qwenConfig = aiAgentProperties.getLlm().getQwen();
    }

    private final Map<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[LangChain4j] Qwen Provider 初始化 - baseUrl: {}, defaultModel: {}", qwenConfig.getBaseUrl(), qwenConfig.getDefaultModel());
        if (qwenConfig.getApiKey() == null || qwenConfig.getApiKey().isBlank()) {
            log.warn("[LangChain4j] Qwen API Key 未配置，通义千问模型将不可用");
        }
    }

    @PreDestroy
    public void destroy() {
        chatModelCache.clear();
        streamingModelCache.clear();
        log.info("[LangChain4j] Qwen Provider 已销毁");
    }

    @Override
    public String getProviderName() {
        return "qwen";
    }

    @Override
    public ChatLanguageModel getChatModel() {
        return getOrCreateChatModel(qwenConfig.getDefaultModel(), 0.7, 0.9, 2048);
    }

    public ChatLanguageModel getOrCreateChatModel(String modelName, double temperature, double topP, int maxTokens) {
        String cacheKey = String.format("%s|%.2f|%.2f|%d", modelName, temperature, topP, maxTokens);
        return chatModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("[LangChain4j] 创建 Qwen ChatModel: model={}, temp={}", modelName, temperature);
            return OpenAiChatModel.builder()
                    .apiKey(qwenConfig.getApiKey())
                    .baseUrl(qwenConfig.getBaseUrl())
                    .modelName(modelName != null ? modelName : qwenConfig.getDefaultModel())
                    .temperature(temperature)
                    .topP(topP)
                    .maxTokens(maxTokens)
                    .timeout(java.time.Duration.ofSeconds(qwenConfig.getTimeoutSeconds()))
                    .logRequests(qwenConfig.getLogRequests())
                    .logResponses(qwenConfig.getLogResponses())
                    .build();
        });
    }

    @Override
    public StreamingChatLanguageModel getStreamingChatModel() {
        String cacheKey = "streaming|" + qwenConfig.getDefaultModel();
        return streamingModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("[LangChain4j] 创建 Qwen StreamingChatModel: model={}", qwenConfig.getDefaultModel());
            return OpenAiStreamingChatModel.builder()
                    .apiKey(qwenConfig.getApiKey())
                    .baseUrl(qwenConfig.getBaseUrl())
                    .modelName(qwenConfig.getDefaultModel())
                    .temperature(0.7)
                    .timeout(java.time.Duration.ofSeconds(qwenConfig.getTimeoutSeconds()))
                    .logRequests(qwenConfig.getLogRequests())
                    .logResponses(qwenConfig.getLogResponses())
                    .build();
        });
    }

    @Override
    public boolean supportsToolCalling() {
        // qwen-max 和 qwen-plus 支持 Function Calling
        return "qwen-max".equals(qwenConfig.getDefaultModel()) || "qwen-plus".equals(qwenConfig.getDefaultModel());
    }

    @Override
    public boolean supportsStreaming() {
        return qwenConfig.getApiKey() != null && !qwenConfig.getApiKey().isBlank();
    }

    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "qwen-turbo", "qwen-plus", "qwen-max", "qwen-long",
                "qwen-vl-plus", "qwen-vl-max", "qwen-audio-turbo"
        );
    }

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
}
