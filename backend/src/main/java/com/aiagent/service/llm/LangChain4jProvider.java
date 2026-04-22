package com.aiagent.service.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;

import java.util.List;
import java.util.concurrent.Flow;

/**
 * LangChain4j 原生 LLM Provider 接口
 * 替代旧版 RestTemplate 手动调用，统一使用 langchain4j ChatLanguageModel
 */
public interface LangChain4jProvider {

    /**
     * 获取提供商标识
     */
    String getProviderName();

    /**
     * 获取同步聊天模型
     */
    ChatLanguageModel getChatModel();

    /**
     * 获取流式聊天模型（可选，不支持时返回 null）
     */
    StreamingChatLanguageModel getStreamingChatModel();

    /**
     * 获取 Token 计数估算器（可选）
     */
    default TokenCountEstimator getTokenCountEstimator() {
        return null;
    }

    /**
     * 同步对话
     */
    default String chat(String systemPrompt, String userMessage) {
        ChatLanguageModel model = getChatModel();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            return model.generate(SystemMessage.from(systemPrompt), UserMessage.from(userMessage))
                    .content().text();
        }
        return model.generate(UserMessage.from(userMessage)).content().text();
    }

    /**
     * 同步对话（带完整消息历史）
     */
    default Response<AiMessage> chat(List<ChatMessage> messages) {
        return getChatModel().generate(messages);
    }

    /**
     * 同步对话（带工具调用支持）
     */
    default Response<AiMessage> chatWithTools(List<ChatMessage> messages,
                                               List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecs) {
        return getChatModel().generate(messages, toolSpecs);
    }

    /**
     * 流式对话
     */
    default void chatStream(String systemPrompt, String userMessage, Flow.Subscriber<String> subscriber) {
        StreamingChatLanguageModel model = getStreamingChatModel();
        if (model == null) {
            // 降级为同步
            String result = chat(systemPrompt, userMessage);
            subscriber.onNext(result);
            subscriber.onComplete();
            return;
        }

        List<ChatMessage> messages;
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages = List.of(SystemMessage.from(systemPrompt), UserMessage.from(userMessage));
        } else {
            messages = List.of(UserMessage.from(userMessage));
        }

        model.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                subscriber.onNext(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                subscriber.onComplete();
            }

            @Override
            public void onError(Throwable error) {
                subscriber.onError(error);
            }
        });
    }

    /**
     * 估算 Token 数量
     */
    default int estimateTokenCount(String text) {
        TokenCountEstimator estimator = getTokenCountEstimator();
        if (estimator != null) {
            return estimator.estimateTokenCount(text);
        }
        // 粗略估算: 中文约 1.5 token/字, 英文约 0.25 token/word
        return (int) (text.length() * 0.75);
    }

    /**
     * 检查模型是否支持函数调用
     */
    default boolean supportsToolCalling() {
        return true;
    }

    /**
     * 检查是否支持流式输出
     */
    default boolean supportsStreaming() {
        return getStreamingChatModel() != null;
    }

    /**
     * 获取可用模型列表
     */
    default List<String> getAvailableModels() {
        return List.of();
    }
}
