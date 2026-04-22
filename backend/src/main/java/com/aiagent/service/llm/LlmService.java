package com.aiagent.service.llm;

import com.aiagent.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final List<LlmProvider> providers;

    public LlmProvider getProvider(String name) {
        return providers.stream()
            .filter(p -> p.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new BusinessException("不支持的LLM提供商: " + name));
    }

    public String chat(String providerName, String systemPrompt, String userMessage, LlmConfig config) {
        LlmProvider provider = getProvider(providerName);
        return provider.chat(systemPrompt, userMessage, config);
    }

    public List<String> getAvailableProviders() {
        return providers.stream().map(LlmProvider::getName).toList();
    }
}
