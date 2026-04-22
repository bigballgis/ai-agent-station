package com.aiagent.service.llm;

public interface LlmProvider {
    String getName();
    String chat(String systemPrompt, String userMessage, LlmConfig config);
    String chatStream(String systemPrompt, String userMessage, LlmConfig config);
}
