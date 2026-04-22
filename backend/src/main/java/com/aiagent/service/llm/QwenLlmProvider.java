package com.aiagent.service.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 旧版通义千问 (Qwen) LLM 提供者实现。
 * <p>
 * 修复说明: 此类为遗留实现，chatStream() 方法返回同步结果而非真正的流式输出。
 * 项目已迁移至 LangChain4j 框架，请使用 {@link LangChain4jService} 替代。
 * </p>
 *
 * @deprecated 请使用 {@link LangChain4jService} 替代，该类通过 LangChain4j 提供真正的流式支持。
 */
@Deprecated
@Component
@RequiredArgsConstructor
public class QwenLlmProvider implements LlmProvider {

    private final RestTemplate restTemplate;

    @Value("${ai-agent.llm.qwen.api-key:}")
    private String apiKey;

    @Value("${ai-agent.llm.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Override
    public String getName() {
        return "qwen";
    }

    @Override
    public String chat(String systemPrompt, String userMessage, LlmConfig config) {
        // 与 OpenAI 兼容的 API 调用
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel() != null ? config.getModel() : "qwen-plus");
        body.put("temperature", config.getTemperature());
        body.put("top_p", config.getTopP());
        body.put("max_tokens", config.getMaxTokens());
        body.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/chat/completions", request, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> choices = (Map<String, Object>) ((List<?>) response.getBody().get("choices")).get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get("message");
        return (String) message.get("content");
    }

    /**
     * 流式聊天 - 修复说明: 此方法名义上是流式调用，但实际返回同步结果。
     * 项目已迁移至 LangChain4j，请使用 LangChain4jService 获取真正的流式支持。
     *
     * @deprecated 此方法返回同步结果，非真正的流式输出。请使用 LangChain4jService 替代。
     */
    @Deprecated
    @Override
    public String chatStream(String systemPrompt, String userMessage, LlmConfig config) {
        // 注意: 流式调用暂返回同步结果，这是一个已知的遗留问题
        return chat(systemPrompt, userMessage, config);
    }
}
