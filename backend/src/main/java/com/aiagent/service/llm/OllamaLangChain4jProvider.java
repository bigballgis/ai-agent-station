package com.aiagent.service.llm;

import com.aiagent.config.properties.AiAgentProperties;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain4j Ollama Provider
 * 支持本地部署的开源大模型: Llama3, Qwen2, Mistral, CodeLlama 等
 *
 * 核心特性:
 * - 使用 langchain4j-ollama 原生驱动
 * - 支持本地 GPU 加速推理
 * - 零成本（无需 API Key）
 * - 数据不出域（隐私安全）
 */
@Slf4j
@Component
public class OllamaLangChain4jProvider implements LangChain4jProvider {

    private final AiAgentProperties.Llm.OllamaConfig ollamaConfig;

    public OllamaLangChain4jProvider(AiAgentProperties aiAgentProperties) {
        this.ollamaConfig = aiAgentProperties.getLlm().getOllama();
    }

    private final Map<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[LangChain4j] Ollama Provider 初始化 - baseUrl: {}, defaultModel: {}", ollamaConfig.getBaseUrl(), ollamaConfig.getDefaultModel());
    }

    @PreDestroy
    public void destroy() {
        chatModelCache.clear();
        streamingModelCache.clear();
        log.info("[LangChain4j] Ollama Provider 已销毁");
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public ChatLanguageModel getChatModel() {
        return getOrCreateChatModel(ollamaConfig.getDefaultModel(), 0.7, 2048);
    }

    public ChatLanguageModel getOrCreateChatModel(String modelName, double temperature, int maxTokens) {
        String cacheKey = String.format("%s|%.2f|%d", modelName, temperature, maxTokens);
        return chatModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("[LangChain4j] 创建 Ollama ChatModel: model={}, temp={}", modelName, temperature);
            OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
                    .baseUrl(ollamaConfig.getBaseUrl())
                    .modelName(modelName != null ? modelName : ollamaConfig.getDefaultModel())
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(ollamaConfig.getTimeoutSeconds()))
                    .numCtx(ollamaConfig.getNumCtx());

            return builder.build();
        });
    }

    @Override
    public StreamingChatLanguageModel getStreamingChatModel() {
        String cacheKey = "streaming|" + ollamaConfig.getDefaultModel();
        return streamingModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("[LangChain4j] 创建 Ollama StreamingChatModel: model={}", ollamaConfig.getDefaultModel());
            return OllamaStreamingChatModel.builder()
                    .baseUrl(ollamaConfig.getBaseUrl())
                    .modelName(ollamaConfig.getDefaultModel())
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(ollamaConfig.getTimeoutSeconds()))
                    .numCtx(ollamaConfig.getNumCtx())
                    .build();
        });
    }

    @Override
    public boolean supportsToolCalling() {
        // Ollama 部分模型支持工具调用（如 llama3.1）
        return true;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "qwen2:7b", "qwen2:72b", "qwen2.5:7b", "qwen2.5:32b",
                "llama3:8b", "llama3:70b", "llama3.1:8b", "llama3.1:70b",
                "mistral:7b", "mixtral:8x7b",
                "codellama:7b", "codellama:13b",
                "gemma2:9b", "deepseek-coder-v2:16b"
        );
    }

    public String chatWithConfig(String systemPrompt, String userMessage, LlmProviderConfig config) {
        ChatLanguageModel model = getOrCreateChatModel(
                config.getModelName(),
                config.getTemperature() != null ? config.getTemperature() : 0.7,
                config.getMaxTokens() != null ? config.getMaxTokens() : 2048
        );

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            return model.generate(SystemMessage.from(systemPrompt), UserMessage.from(userMessage))
                    .content().text();
        }
        return model.generate(UserMessage.from(userMessage)).content().text();
    }
}
