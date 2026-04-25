package com.aiagent.service.llm;

import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ServiceUnavailableException;
import com.aiagent.exception.ValidationException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;

/**
 * LangChain4j 统一 LLM 服务
 * 
 * 核心能力:
 * 1. Provider 路由 - 根据 providerName 自动选择对应的 LangChain4j Provider
 * 2. ChatMemory 管理 - 基于 sessionId 的对话记忆，支持窗口大小配置
 * 3. Tool Calling 循环 - 自动处理 LLM 返回的工具调用请求
 * 4. 流式输出 - SSE/WebSocket 流式响应
 * 5. Token 估算 - 对话 Token 消耗统计
 * 
 * 架构设计:
 * LangChain4jProvider (接口)
 *   ├── OpenAiLangChain4jProvider (GPT-4/4o/3.5)
 *   ├── QwenLangChain4jProvider (通义千问)
 *   └── OllamaLangChain4jProvider (本地开源模型)
 */
@Slf4j
@Service
public class LangChain4jService {

    private final List<LangChain4jProvider> providers;
    private final Map<String, LangChain4jProvider> providerMap = new ConcurrentHashMap<>();

    /**
     * ChatMemory 存储（Redis 持久化，支持多实例部署和应用重启后恢复）
     */
    private final ChatMemoryStore memoryStore;

    /**
     * 活跃的 ChatMemory 实例缓存: key = memoryId (通常是 sessionId/agentId)
     */
    private final Map<String, ChatMemory> memoryCache = new ConcurrentHashMap<>();

    public LangChain4jService(List<LangChain4jProvider> providers, ChatMemoryStore memoryStore) {
        this.providers = providers;
        this.memoryStore = memoryStore;
        for (LangChain4jProvider provider : providers) {
            providerMap.put(provider.getProviderName().toLowerCase(), provider);
            log.info("[LangChain4j] 注册 Provider: {} - supportsToolCalling: {}, supportsStreaming: {}",
                    provider.getProviderName(), provider.supportsToolCalling(), provider.supportsStreaming());
        }
        log.info("[LangChain4j] 共注册 {} 个 LLM Provider", providers.size());
    }

    // ==================== Provider 路由 ====================

    /**
     * 获取指定名称的 Provider
     */
    public LangChain4jProvider getProvider(String providerName) {
        LangChain4jProvider provider = providerMap.get(providerName.toLowerCase());
        if (provider == null) {
            throw new ValidationException("不支持的 LLM Provider: " + providerName +
                    ", 可用: " + String.join(", ", providerMap.keySet()));
        }
        return provider;
    }

    /**
     * 获取所有可用 Provider
     */
    public List<LangChain4jProvider> getAllProviders() {
        return new ArrayList<>(providers);
    }

    /**
     * 获取所有可用 Provider 名称
     */
    public List<String> getAvailableProviderNames() {
        return providers.stream().map(LangChain4jProvider::getProviderName).toList();
    }

    // ==================== 同步对话 ====================

    /**
     * 简单同步对话（无记忆、无工具）
     * 含 Spring Retry: 最多重试3次，指数退避 (1s, 2s, 4s)
     */
    @Retryable(
        value = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "chatRecover"
    )
    public String chat(String providerName, String systemPrompt, String userMessage) {
        LangChain4jProvider provider = getProvider(providerName);
        return provider.chat(systemPrompt, userMessage);
    }

    /**
     * chat() 方法的恢复方法 - 重试耗尽后返回降级响应
     */
    private String chatRecover(RuntimeException e, String providerName, String systemPrompt, String userMessage) {
        log.error("[LangChain4j] chat() 重试耗尽, provider={}, error={}", providerName, e.getMessage());
        throw new ServiceUnavailableException("LLM-Chat", e.getMessage(), e);
    }

    /**
     * 带配置的同步对话
     * 含 Spring Retry: 最多重试3次，指数退避 (1s, 2s, 4s)
     */
    @Retryable(
        value = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "chatWithConfigRecover"
    )
    public String chatWithConfig(String providerName, String systemPrompt, String userMessage, LlmProviderConfig config) {
        LangChain4jProvider provider = getProvider(providerName);
        if (provider instanceof OpenAiLangChain4jProvider openAi) {
            return openAi.chatWithConfig(systemPrompt, userMessage, config);
        } else if (provider instanceof QwenLangChain4jProvider qwen) {
            return qwen.chatWithConfig(systemPrompt, userMessage, config);
        } else if (provider instanceof OllamaLangChain4jProvider ollama) {
            return ollama.chatWithConfig(systemPrompt, userMessage, config);
        }
        return provider.chat(systemPrompt, userMessage);
    }

    /**
     * chatWithConfig() 方法的恢复方法 - 重试耗尽后抛出异常
     */
    private String chatWithConfigRecover(RuntimeException e, String providerName, String systemPrompt,
                                          String userMessage, LlmProviderConfig config) {
        log.error("[LangChain4j] chatWithConfig() 重试耗尽, provider={}, error={}", providerName, e.getMessage());
        throw new ServiceUnavailableException("LLM-ChatWithConfig", e.getMessage(), e);
    }

    /**
     * 带消息历史的同步对话
     * 含 Spring Retry: 最多重试3次，指数退避 (1s, 2s, 4s)
     */
    @Retryable(
        value = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "chatWithHistoryRecover"
    )
    public Response<AiMessage> chatWithHistory(String providerName, List<ChatMessage> messages) {
        LangChain4jProvider provider = getProvider(providerName);
        return provider.chat(messages);
    }

    /**
     * chatWithHistory() 方法的恢复方法
     */
    private Response<AiMessage> chatWithHistoryRecover(RuntimeException e, String providerName,
                                                        List<ChatMessage> messages) {
        log.error("[LangChain4j] chatWithHistory() 重试耗尽, provider={}, error={}", providerName, e.getMessage());
        throw new ServiceUnavailableException("LLM-ChatWithHistory", e.getMessage(), e);
    }

    // ==================== ChatMemory 管理 ====================

    /**
     * 获取或创建 ChatMemory
     * @param memoryId 会话标识（通常是 sessionId 或 agentId:sessionId）
     * @param maxMessages 最大保留消息数（窗口大小）
     */
    public ChatMemory getOrCreateMemory(String memoryId, int maxMessages) {
        return memoryCache.computeIfAbsent(memoryId, key -> {
            log.debug("[LangChain4j] 创建 ChatMemory: memoryId={}, maxMessages={}", memoryId, maxMessages);
            return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .chatMemoryStore(memoryStore)
                    .maxMessages(maxMessages)
                    .build();
        });
    }

    /**
     * 带记忆的对话
     * 含 Spring Retry: 最多重试3次，指数退避 (1s, 2s, 4s)
     */
    @Retryable(
        value = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "chatWithMemoryRecover"
    )
    public String chatWithMemory(String providerName, String memoryId, String userMessage, int maxMessages) {
        LangChain4jProvider provider = getProvider(providerName);
        ChatMemory memory = getOrCreateMemory(memoryId, maxMessages);

        memory.add(UserMessage.from(userMessage));
        Response<AiMessage> response = provider.chat(memory.messages());
        memory.add(response.content());

        return response.content().text();
    }

    /**
     * 带记忆和系统提示的对话
     * 含 Spring Retry: 最多重试3次，指数退避 (1s, 2s, 4s)
     */
    @Retryable(
        value = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "chatWithMemorySystemRecover"
    )
    public String chatWithMemory(String providerName, String memoryId, String systemPrompt,
                                  String userMessage, int maxMessages) {
        LangChain4jProvider provider = getProvider(providerName);
        ChatMemory memory = getOrCreateMemory(memoryId, maxMessages);

        // 确保系统提示在记忆中
        List<ChatMessage> messages = memory.messages();
        if (messages.isEmpty() || !(messages.get(0) instanceof SystemMessage)) {
            memory.add(SystemMessage.from(systemPrompt));
        }

        memory.add(UserMessage.from(userMessage));
        Response<AiMessage> response = provider.chat(memory.messages());
        memory.add(response.content());

        return response.content().text();
    }

    /**
     * chatWithMemory(4参数) 方法的恢复方法
     */
    private String chatWithMemoryRecover(RuntimeException e, String providerName, String memoryId,
                                          String userMessage, int maxMessages) {
        log.error("[LangChain4j] chatWithMemory() 重试耗尽, provider={}, memoryId={}, error={}",
                providerName, memoryId, e.getMessage());
        throw new ServiceUnavailableException("LLM-ChatWithMemory", e.getMessage(), e);
    }

    /**
     * chatWithMemory(5参数,含systemPrompt) 方法的恢复方法
     */
    private String chatWithMemorySystemRecover(RuntimeException e, String providerName, String memoryId,
                                                String systemPrompt, String userMessage, int maxMessages) {
        log.error("[LangChain4j] chatWithMemory(systemPrompt) 重试耗尽, provider={}, memoryId={}, error={}",
                providerName, memoryId, e.getMessage());
        throw new ServiceUnavailableException("LLM-ChatWithMemorySystem", e.getMessage(), e);
    }

    /**
     * 清除指定会话的记忆
     */
    public void clearMemory(String memoryId) {
        ChatMemory memory = memoryCache.remove(memoryId);
        if (memory != null) {
            memory.clear();
            log.debug("[LangChain4j] 已清除 ChatMemory: memoryId={}", memoryId);
        }
    }

    /**
     * 获取会话历史
     */
    public List<ChatMessage> getMemoryMessages(String memoryId) {
        ChatMemory memory = memoryCache.get(memoryId);
        return memory != null ? memory.messages() : List.of();
    }

    // ==================== Tool Calling ====================

    /**
     * 带 Tool Calling 的对话（自动执行工具调用循环）
     * 
     * 工作流程:
     * 1. LLM 接收消息 + 工具定义
     * 2. 如果 LLM 返回工具调用请求 → 执行工具 → 将结果反馈给 LLM
     * 3. 重复步骤 2 直到 LLM 返回文本响应（或达到最大工具调用次数）
     * 
     * 含 Spring Retry: 最多重试3次，指数退避 (1s, 2s, 4s)
     * 
     * @param providerName Provider 名称
     * @param messages 对话历史
     * @param toolSpecs 工具定义列表
     * @param toolExecutor 工具执行器（接收工具名和参数，返回执行结果）
     * @param maxToolIterations 最大工具调用迭代次数（防止无限循环）
     */
    @Retryable(
        value = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "chatWithToolsRecover"
    )
    public Response<AiMessage> chatWithTools(String providerName, List<ChatMessage> messages,
                                              List<ToolSpecification> toolSpecs,
                                              ToolExecutor toolExecutor,
                                              int maxToolIterations) {
        LangChain4jProvider provider = getProvider(providerName);

        List<ChatMessage> currentMessages = new ArrayList<>(messages);
        int iteration = 0;

        while (iteration < maxToolIterations) {
            Response<AiMessage> response = provider.chatWithTools(currentMessages, toolSpecs);
            AiMessage aiMessage = response.content();

            if (!aiMessage.hasToolExecutionRequests()) {
                // LLM 返回文本响应，结束循环
                return response;
            }

            // 处理工具调用
            currentMessages.add(aiMessage);
            for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                log.debug("[LangChain4j] 执行工具: {}({})", toolRequest.name(), toolRequest.arguments());
                try {
                    String result = toolExecutor.execute(toolRequest.name(), toolRequest.arguments());
                    currentMessages.add(ToolExecutionResultMessage.from(toolRequest, result));
                } catch (BusinessException e) {
                    log.error("[LangChain4j] 工具执行业务异常: {} - {}", toolRequest.name(), e.getMessage());
                    currentMessages.add(ToolExecutionResultMessage.from(toolRequest,
                            "工具执行失败: " + e.getMessage()));
                } catch (Exception e) {
                    log.error("[LangChain4j] 工具执行未知异常: {} - {}", toolRequest.name(), e.getMessage(), e);
                    currentMessages.add(ToolExecutionResultMessage.from(toolRequest,
                            "工具执行失败(内部错误): " + e.getMessage()));
                }
            }
            iteration++;
        }

        log.warn("[LangChain4j] 达到最大工具调用迭代次数: {}", maxToolIterations);
        // 最后一次调用，不传工具定义，强制 LLM 返回文本
        return provider.chat(currentMessages);
    }

    /**
     * chatWithTools() 方法的恢复方法
     */
    private Response<AiMessage> chatWithToolsRecover(RuntimeException e, String providerName,
                                                      List<ChatMessage> messages,
                                                      List<ToolSpecification> toolSpecs,
                                                      ToolExecutor toolExecutor,
                                                      int maxToolIterations) {
        log.error("[LangChain4j] chatWithTools() 重试耗尽, provider={}, error={}", providerName, e.getMessage());
        throw new ServiceUnavailableException("LLM-ChatWithTools", e.getMessage(), e);
    }

    /**
     * 工具执行器函数式接口
     */
    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String toolName, String toolArguments);
    }

    // ==================== 流式输出 ====================

    /**
     * 流式对话
     */
    public void chatStream(String providerName, String systemPrompt, String userMessage,
                           Flow.Subscriber<String> subscriber) {
        LangChain4jProvider provider = getProvider(providerName);
        provider.chatStream(systemPrompt, userMessage, subscriber);
    }

    /**
     * 带记忆的流式对话
     */
    public void chatStreamWithMemory(String providerName, String memoryId, String userMessage,
                                      int maxMessages, Flow.Subscriber<String> subscriber) {
        LangChain4jProvider provider = getProvider(providerName);
        ChatMemory memory = getOrCreateMemory(memoryId, maxMessages);
        memory.add(UserMessage.from(userMessage));

        StringBuilder fullResponse = new StringBuilder();
        provider.chatStream(null, userMessage, new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                subscriber.onNext(token);
            }

            @Override
            public void onError(Throwable throwable) {
                subscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                memory.add(AiMessage.from(fullResponse.toString()));
                subscriber.onComplete();
            }
        });
    }

    // ==================== Token 估算 ====================

    /**
     * 估算 Token 消耗
     */
    public int estimateTokens(String providerName, String text) {
        LangChain4jProvider provider = getProvider(providerName);
        return provider.estimateTokenCount(text);
    }

    /**
     * 估算对话总 Token 消耗
     */
    public int estimateConversationTokens(String providerName, List<ChatMessage> messages) {
        LangChain4jProvider provider = getProvider(providerName);
        int total = 0;
        for (ChatMessage message : messages) {
            total += provider.estimateTokenCount(message.toString());
        }
        return total;
    }

    // ==================== 兼容旧版 LlmService ====================

    /**
     * 兼容旧版 LlmService.chat() 调用
     * @deprecated 使用 chat() 替代
     */
    @Deprecated
    public String legacyChat(String providerName, String systemPrompt, String userMessage, LlmConfig config) {
        LlmProviderConfig providerConfig = LlmProviderConfig.fromLegacy(config, providerName);
        return chatWithConfig(providerName, systemPrompt, userMessage, providerConfig);
    }
}
