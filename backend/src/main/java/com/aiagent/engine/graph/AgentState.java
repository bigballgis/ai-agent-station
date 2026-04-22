package com.aiagent.engine.graph;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 执行状态 — 类型化的状态容器（替代 Map<String, Object>）
 */
@Data
public class AgentState {
    /** 原始用户输入 */
    private Map<String, Object> inputs = new HashMap<>();
    /** 处理后的输入 */
    private Map<String, Object> processedInput = new HashMap<>();
    /** LLM 原始输出 */
    private String llmOutput;
    /** LLM 结构化输出（JSON） */
    private Map<String, Object> structuredOutput;
    /** 工具调用结果 */
    private Map<String, Object> toolResults = new HashMap<>();
    /** 条件分支结果 */
    private String conditionResult;  // "true", "false", or custom value
    /** 变量存储 */
    private Map<String, Object> variables = new HashMap<>();
    /** 记忆上下文 */
    private String memoryContext;
    /** 检索结果 */
    private Map<String, Object> retrievalResults;
    /** 最终输出 */
    private Map<String, Object> outputs = new HashMap<>();
    /** 错误信息 */
    private String error;
    /** 当前执行节点 ID */
    private String currentNodeId;
    /** 是否应该终止 */
    private boolean shouldTerminate;
    /** 最大迭代次数（防止无限循环） */
    private int maxIterations = 25;
    /** 当前迭代计数 */
    private int iterationCount;
    /** Agent ID */
    private Long agentId;
    /** 执行 ID（用于追踪） */
    private String executionId;
    /** 租户 ID */
    private Long tenantId;
    /** 用户 ID */
    private Long userId;

    public void setVariable(String key, Object value) {
        this.variables.put(key, value);
    }

    public Object getVariable(String key) {
        return this.variables.get(key);
    }

    public void incrementIteration() {
        this.iterationCount++;
    }

    public boolean isMaxIterationsReached() {
        return this.iterationCount >= this.maxIterations;
    }
}
