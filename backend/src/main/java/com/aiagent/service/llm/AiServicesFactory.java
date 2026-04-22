package com.aiagent.service.llm;

import com.aiagent.service.tool.CompositeToolProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain4j AiServices 工厂
 * 
 * 负责创建和管理 AiServices Agent 实例。
 * 默认注入 CompositeToolProvider（MCP + Function 双通道）。
 * 每个 Agent 可以有不同的系统提示、模型、记忆和工具集。
 * 
 * 核心概念:
 * - AiServices: langchain4j 的声明式 AI Agent 框架
 * - 通过 Java 接口定义 Agent 行为
 * - 自动处理记忆管理、工具调用、流式输出
 * 
 * 使用场景:
 * - 不同业务场景使用不同角色设定的 Agent
 * - 多租户隔离（每个租户独立的 Agent 实例）
 * - A/B 测试（同一 Agent 使用不同模型）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiServicesFactory {

    private final LangChain4jService langChain4jService;
    private final CompositeToolProvider compositeToolProvider;

    /**
     * Agent 实例缓存: key = agentId
     */
    private final Map<String, AgentAssistant> agentCache = new ConcurrentHashMap<>();

    /**
     * Agent 配置缓存
     */
    private final Map<String, AgentAiServicesConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[AiServices] AiServicesFactory 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        agentCache.clear();
        configCache.clear();
        log.info("[AiServices] AiServicesFactory 已销毁");
    }

    /**
     * 获取或创建 Agent 实例
     * 
     * @param agentId Agent 唯一标识
     * @param config Agent 配置（provider, model, systemPrompt, memorySize, toolProvider）
     * @return AgentAssistant 实例
     */
    public AgentAssistant getOrCreateAgent(String agentId, AgentAiServicesConfig config) {
        return agentCache.computeIfAbsent(agentId, key -> {
            log.info("[AiServices] 创建 Agent 实例: agentId={}, provider={}, model={}",
                    agentId, config.getProviderName(), config.getModelName());
            configCache.put(key, config);
            return buildAgent(config);
        });
    }

    /**
     * 获取已存在的 Agent 实例
     */
    public AgentAssistant getAgent(String agentId) {
        AgentAssistant agent = agentCache.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent 不存在: " + agentId);
        }
        return agent;
    }

    /**
     * 重建 Agent（配置变更时调用）
     */
    public AgentAssistant rebuildAgent(String agentId, AgentAiServicesConfig newConfig) {
        agentCache.remove(agentId);
        configCache.remove(agentId);
        log.info("[AiServices] 重建 Agent 实例: agentId={}", agentId);
        return getOrCreateAgent(agentId, newConfig);
    }

    /**
     * 销毁 Agent 实例
     */
    public void destroyAgent(String agentId) {
        agentCache.remove(agentId);
        configCache.remove(agentId);
        langChain4jService.clearMemory(agentId);
        log.info("[AiServices] 已销毁 Agent: agentId={}", agentId);
    }

    /**
     * 构建 AiServices Agent 实例
     */
    private AgentAssistant buildAgent(AgentAiServicesConfig config) {
        LangChain4jProvider provider = langChain4jService.getProvider(config.getProviderName());
        ChatLanguageModel chatModel = provider.getChatModel();
        ChatMemory memory = langChain4jService.getOrCreateMemory(
                config.getAgentId(), config.getMemorySize());

        AiServices<AgentAssistant> builder = AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(memory);

        // 添加工具提供者：优先使用配置的，否则默认使用 CompositeToolProvider（MCP + Function）
        ToolProvider toolProvider = config.getToolProvider() != null
                ? config.getToolProvider()
                : compositeToolProvider;
        builder.toolProvider(toolProvider);

        return builder.build();
    }

    /**
     * Agent AiServices 配置
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AgentAiServicesConfig {
        private String agentId;
        private String providerName;
        private String modelName;
        private String systemPrompt;
        @lombok.Builder.Default
        private int memorySize = 20;
        private ToolProvider toolProvider;
    }
}
