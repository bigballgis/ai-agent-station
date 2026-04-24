package com.aiagent.util;

import com.aiagent.entity.Agent;
import com.aiagent.service.tool.FunctionToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent 配置验证工具类
 *
 * 验证规则:
 * 1. LLM 模型名称有效性检查
 * 2. Temperature 范围 [0, 2]
 * 3. MaxTokens 范围 [1, 32000]
 * 4. LLM 类型 Agent 的 systemPrompt 不能为空
 * 5. 工具引用有效性检查（工具必须存在于 FunctionToolRegistry）
 */
@Slf4j
@Component
public class AgentConfigValidator {

    /** 合法的 LLM 模型名称前缀 */
    private static final Set<String> VALID_MODEL_PREFIXES = Set.of(
            "gpt-", "gpt4", "gpt-4", "gpt-3.5", "chatgpt",
            "claude-", "claude",
            "qwen-", "qwen",
            "llama-", "llama",
            "mistral-", "mistral",
            "gemini-", "gemini",
            "deepseek-", "deepseek",
            "glm-", "glm",
            "o1-", "o1", "o3-", "o3", "o4-", "o4"
    );

    /** 已知的完整模型名称 */
    private static final Set<String> KNOWN_MODELS = Set.of(
            "gpt-4", "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4-32k",
            "gpt-3.5-turbo", "gpt-3.5-turbo-16k",
            "claude-3-opus", "claude-3-sonnet", "claude-3-haiku",
            "claude-3-5-sonnet", "claude-3-5-haiku",
            "qwen-plus", "qwen-turbo", "qwen-max", "qwen-long",
            "llama-3-70b", "llama-3-8b",
            "mistral-7b", "mistral-large",
            "gemini-pro", "gemini-1.5-pro", "gemini-1.5-flash",
            "deepseek-chat", "deepseek-coder",
            "glm-4", "glm-4-flash",
            "o1-preview", "o1-mini", "o3-mini", "o4-mini"
    );

    private final FunctionToolRegistry functionToolRegistry;

    public AgentConfigValidator(FunctionToolRegistry functionToolRegistry) {
        this.functionToolRegistry = functionToolRegistry;
    }

    /**
     * 验证 Agent 配置，返回所有验证错误
     *
     * @param agent Agent 实体
     * @return 验证错误列表，如果为空则验证通过
     */
    public List<String> validate(Agent agent) {
        List<String> errors = new ArrayList<>();
        if (agent == null) {
            errors.add("Agent 不能为空");
            return errors;
        }

        Map<String, Object> config = agent.getConfig();
        if (config == null) {
            config = new HashMap<>();
        }

        // 1. 验证模型名称
        validateModelName(config, errors);

        // 2. 验证 Temperature
        validateTemperature(config, errors);

        // 3. 验证 MaxTokens
        validateMaxTokens(config, errors);

        // 4. 验证 SystemPrompt（LLM 类型 Agent）
        validateSystemPrompt(config, errors);

        // 5. 验证工具引用
        validateToolReferences(config, errors);

        return errors;
    }

    /**
     * 验证 Agent 配置，如果不通过则抛出 IllegalArgumentException
     *
     * @param agent Agent 实体
     * @throws IllegalArgumentException 如果验证不通过
     */
    public void validateOrThrow(Agent agent) {
        List<String> errors = validate(agent);
        if (!errors.isEmpty()) {
            String message = "Agent 配置验证失败: " + String.join("; ", errors);
            log.warn(message);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 仅验证配置 Map（不依赖 Agent 实体）
     */
    public List<String> validateConfig(Map<String, Object> config) {
        List<String> errors = new ArrayList<>();
        if (config == null) {
            return errors;
        }

        validateModelName(config, errors);
        validateTemperature(config, errors);
        validateMaxTokens(config, errors);
        validateSystemPrompt(config, errors);
        validateToolReferences(config, errors);

        return errors;
    }

    // ==================== 私有验证方法 ====================

    private void validateModelName(Map<String, Object> config, List<String> errors) {
        Object modelObj = config.get("model");
        if (modelObj == null || modelObj.toString().isBlank()) {
            // model 为空时使用默认值，不报错
            return;
        }
        String model = modelObj.toString().trim().toLowerCase();
        if (KNOWN_MODELS.contains(model)) {
            return;
        }
        // 检查模型名称前缀
        boolean validPrefix = VALID_MODEL_PREFIXES.stream().anyMatch(model::startsWith);
        if (!validPrefix) {
            errors.add("模型名称 '" + modelObj + "' 不是已知的有效模型");
        }
    }

    private void validateTemperature(Map<String, Object> config, List<String> errors) {
        Object tempObj = config.get("temperature");
        if (tempObj == null) {
            return;
        }
        try {
            double temperature = Double.parseDouble(tempObj.toString());
            if (temperature < 0 || temperature > 2) {
                errors.add("Temperature 必须在 [0, 2] 范围内，当前值: " + temperature);
            }
        } catch (NumberFormatException e) {
            errors.add("Temperature 格式无效: '" + tempObj + "'，必须是数字");
        }
    }

    private void validateMaxTokens(Map<String, Object> config, List<String> errors) {
        Object tokensObj = config.get("maxTokens");
        if (tokensObj == null) {
            return;
        }
        try {
            int maxTokens = Integer.parseInt(tokensObj.toString());
            if (maxTokens < 1 || maxTokens > 32000) {
                errors.add("MaxTokens 必须在 [1, 32000] 范围内，当前值: " + maxTokens);
            }
        } catch (NumberFormatException e) {
            errors.add("MaxTokens 格式无效: '" + tokensObj + "'，必须是整数");
        }
    }

    private void validateSystemPrompt(Map<String, Object> config, List<String> errors) {
        Object typeObj = config.get("type");
        String type = typeObj != null ? typeObj.toString().trim().toLowerCase() : "";

        // 仅对 LLM 类型 Agent 检查 systemPrompt
        if (!type.isEmpty() && !type.equals("llm") && !type.equals("chat")) {
            return;
        }

        Object promptObj = config.get("systemPrompt");
        if (promptObj == null || promptObj.toString().isBlank()) {
            // 如果 type 为空（默认 LLM 类型）且有 systemPrompt 字段但为空，给出警告
            if (type.equals("llm") || type.equals("chat")) {
                errors.add("LLM 类型 Agent 的 systemPrompt 不能为空");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateToolReferences(Map<String, Object> config, List<String> errors) {
        Object toolsObj = config.get("tools");
        if (toolsObj == null) {
            return;
        }

        List<String> toolNames;
        if (toolsObj instanceof List) {
            toolNames = (List<String>) toolsObj;
        } else if (toolsObj instanceof String) {
            // 支持逗号分隔的字符串格式
            toolNames = Arrays.asList(toolsObj.toString().split(","));
        } else {
            errors.add("tools 配置格式无效，应为字符串列表");
            return;
        }

        for (String toolName : toolNames) {
            String trimmed = toolName.trim();
            if (!trimmed.isEmpty() && !functionToolRegistry.hasTool(trimmed)) {
                errors.add("工具 '" + trimmed + "' 未在工具注册中心找到");
            }
        }
    }
}
