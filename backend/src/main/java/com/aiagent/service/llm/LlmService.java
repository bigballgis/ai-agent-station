package com.aiagent.service.llm;

import com.aiagent.exception.BusinessException;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 旧版 LLM 服务，基于 LlmProvider 接口。
 * <p>
 * 此类已废弃，不再注册为 Spring Bean。
 * 所有 LlmProvider 实现已迁移至 LangChain4j 框架。
 * 请使用 {@link LangChain4jService} 替代。
 * </p>
 *
 * @deprecated 请使用 {@link LangChain4jService} 替代。
 */
@Deprecated
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
