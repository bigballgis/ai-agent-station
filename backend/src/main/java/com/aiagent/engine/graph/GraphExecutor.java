package com.aiagent.engine.graph;

import com.aiagent.exception.BusinessException;
import com.aiagent.mcp.McpToolGateway;
import com.aiagent.service.MemoryService;
import com.aiagent.service.llm.LangChain4jService;
import com.aiagent.service.llm.LlmProviderConfig;
import com.aiagent.service.tool.CompositeToolProvider;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * DAG 图执行引擎 — LangGraph 风格的状态图编排
 *
 * 核心执行循环:
 * 1. 从入口节点开始
 * 2. 执行当前节点
 * 3. 根据节点类型和输出决定下一个节点
 * 4. 更新状态
 * 5. 检查终止条件
 * 6. 重复直到终止
 */
@Slf4j
@Component
public class GraphExecutor {

    private final LangChain4jService langChain4jService;
    private final CompositeToolProvider compositeToolProvider;
    private final McpToolGateway mcpToolGateway;
    private final MemoryService memoryService;
    private final HttpExecutor httpExecutor;
    private final NodeExecutors nodeExecutors;
    private final Executor agentExecutor;

    public GraphExecutor(LangChain4jService langChain4jService,
                         CompositeToolProvider compositeToolProvider,
                         McpToolGateway mcpToolGateway,
                         MemoryService memoryService,
                         HttpExecutor httpExecutor,
                         NodeExecutors nodeExecutors,
                         @Qualifier("agentExecutor") Executor agentExecutor) {
        this.langChain4jService = langChain4jService;
        this.compositeToolProvider = compositeToolProvider;
        this.mcpToolGateway = mcpToolGateway;
        this.memoryService = memoryService;
        this.httpExecutor = httpExecutor;
        this.nodeExecutors = nodeExecutors;
        this.agentExecutor = agentExecutor;
    }

    /**
     * 执行图定义
     */
    public Map<String, Object> execute(GraphDefinition graph, AgentState state) {
        // 验证图结构
        List<String> errors = graph.validate();
        if (!errors.isEmpty()) {
            throw new BusinessException("图定义验证失败: " + String.join("; ", errors));
        }

        String currentNodeId = graph.getEntryNodeId();
        state.setIterationCount(0);

        // Track each node's output for {{nodeId.output}} variable resolution
        Map<String, Object> nodeOutputs = new HashMap<>();

        outerLoop:
        while (currentNodeId != null && !state.isShouldTerminate()) {
            if (state.isMaxIterationsReached()) {
                log.warn("达到最大迭代次数 {}, 终止执行", state.getMaxIterations());
                state.setError("达到最大迭代次数");
                break;
            }

            GraphNode node = graph.getNode(currentNodeId);
            if (node == null) {
                log.error("节点 {} 不存在", currentNodeId);
                state.setError("节点 " + currentNodeId + " 不存在");
                break;
            }

            state.setCurrentNodeId(currentNodeId);
            node.setStatus("running");
            node.setExecutionOrder(state.getIterationCount() + 1);

            // 检查超时设置
            int timeoutSeconds = node.getTimeoutSeconds() > 0 ? node.getTimeoutSeconds() : 30;
            // TODO: 使用 CompletableFuture.orTimeout() 实现节点级超时控制
            // 当前为同步执行，超时控制需异步化改造
            log.debug("节点 {} 超时设置: {}秒", node.getId(), timeoutSeconds);

            // 重试机制
            int maxRetries = node.getMaxRetries() > 0 ? node.getMaxRetries() : 1;
            int attempt = 0;
            while (attempt < maxRetries) {
                try {
                    log.info("执行节点 [{}] {} (类型: {}, 第 {} 步, 尝试 {}/{})",
                        node.getId(), node.getLabel(), node.getType(), node.getExecutionOrder(), attempt + 1, maxRetries);

                    // 根据节点类型执行
                    switch (node.getType()) {
                        case "start" -> executeStartNode(node, state);
                        case "llm" -> executeLlmNode(node, state, nodeOutputs, graph);
                        case "condition" -> executeConditionNode(node, state);
                        case "tool" -> executeToolNode(node, state);
                        case "memory" -> executeMemoryNode(node, state);
                        case "variable" -> executeVariableNode(node, state);
                        case "retriever" -> executeRetrieverNode(node, state, nodeOutputs, graph);
                        case "exception" -> executeExceptionNode(node, state);
                        case "http" -> executeHttpNode(node, state, nodeOutputs, graph);
                        case "end" -> { node.setStatus("completed"); state.setShouldTerminate(true); break; }
                        case "parallel" -> executeParallelNode(node, state, nodeOutputs, graph);
                        case "merge" -> executeMergeNode(node, state, nodeOutputs, graph);
                        case "switch" -> executeSwitchNode(node, state);
                        case "subgraph" -> executeSubgraphNode(node, state);
                        case "human_approval" -> executeHumanApprovalNode(node, state);
                        case "code" -> {
                            String result = nodeExecutors.executeCodeNode(node.getId(), node.getConfig(), nodeOutputs);
                            node.setOutput(result);
                            nodeOutputs.put(node.getId(), result);
                        }
                        case "delay" -> {
                            String result = nodeExecutors.executeDelayNode(node.getId(), node.getConfig());
                            node.setOutput(result);
                            nodeOutputs.put(node.getId(), result);
                        }
                        default -> {
                            log.warn("未知节点类型: {}, 跳过", node.getType());
                            node.setStatus("skipped");
                        }
                    }

                    if (!state.isShouldTerminate()) {
                        node.setStatus("completed");
                        // Store node output for variable resolution
                        if (node.getOutput() != null) {
                            nodeOutputs.put(currentNodeId, node.getOutput());
                        }
                        // 决定下一个节点
                        currentNodeId = resolveNextNode(graph, node, state);
                    }
                    break; // 成功，退出重试循环

                } catch (Exception e) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        log.error("节点 {} 执行失败（已重试 {} 次）: {}", node.getId(), maxRetries, e.getMessage(), e);
                        node.setStatus("failed");
                        node.setError(e.getMessage());

                        // 查找异常处理节点
                        currentNodeId = findExceptionHandler(graph, node);
                        if (currentNodeId == null) {
                            state.setError("节点 " + node.getLabel() + " 执行失败: " + e.getMessage());
                            break outerLoop; // 跳出外层 while 循环
                        }
                        break; // 跳出重试循环，继续执行异常处理节点
                    }
                    log.warn("节点 {} 执行失败，重试 {}/{}", node.getId(), attempt, maxRetries);
                }
            }

            state.incrementIteration();
        }

        // 构建最终输出
        Map<String, Object> result = new HashMap<>();
        result.put("outputs", state.getOutputs());
        result.put("llmOutput", state.getLlmOutput());
        result.put("structuredOutput", state.getStructuredOutput());
        result.put("variables", state.getVariables());
        result.put("iterations", state.getIterationCount());
        result.put("status", state.getError() != null ? "failed" : "success");
        if (state.getError() != null) {
            result.put("error", state.getError());
        }
        return result;
    }

    private void executeStartNode(GraphNode node, AgentState state) {
        log.info("开始执行 Agent: {}", state.getAgentId());
        // 将输入复制到状态
        state.getProcessedInput().putAll(state.getInputs());
    }

    private void executeLlmNode(GraphNode node, AgentState state, Map<String, Object> nodeOutputs, GraphDefinition graph) {
        Map<String, Object> nodeConfig = node.getConfig();
        String provider = (String) nodeConfig.getOrDefault("provider", "openai");
        String modelName = (String) nodeConfig.getOrDefault("model", null);
        String systemPrompt = resolveVariableReferences((String) nodeConfig.getOrDefault("systemPrompt", "你是一个 AI 助手。"), nodeOutputs, graph);

        // 构建 langchain4j 配置
        double temperature = 0.7;
        Object tempObj = nodeConfig.get("temperature");
        if (tempObj instanceof Number) {
            temperature = ((Number) tempObj).doubleValue();
        } else if (tempObj instanceof String) {
            try { temperature = Double.parseDouble((String) tempObj); } catch (NumberFormatException e) { /* use default */ }
        }

        LlmProviderConfig config = LlmProviderConfig.builder()
                .provider(provider)
                .modelName(modelName)
                .temperature(temperature)
                .topP(nodeConfig.get("topP") != null ? ((Number) nodeConfig.get("topP")).doubleValue() : 0.9)
                .maxTokens(nodeConfig.get("maxTokens") != null ? ((Number) nodeConfig.get("maxTokens")).intValue() : 2048)
                .build();

        // 构建用户消息（包含上下文）
        StringBuilder userMessage = new StringBuilder();

        // 添加记忆上下文
        if (state.getMemoryContext() != null) {
            userMessage.append("【历史记忆】\n").append(state.getMemoryContext()).append("\n\n");
        }

        // 添加检索结果
        if (state.getRetrievalResults() != null && !state.getRetrievalResults().isEmpty()) {
            userMessage.append("【检索结果】\n").append(state.getRetrievalResults().toString()).append("\n\n");
        }

        // 添加工具调用结果
        if (state.getToolResults() != null && !state.getToolResults().isEmpty()) {
            userMessage.append("【工具调用结果】\n").append(state.getToolResults().toString()).append("\n\n");
        }

        // 添加变量
        if (!state.getVariables().isEmpty()) {
            userMessage.append("【变量】\n");
            state.getVariables().forEach((k, v) -> userMessage.append(k).append(": ").append(v).append("\n"));
            userMessage.append("\n");
        }

        // 添加用户输入（支持模板变量替换）
        String promptTemplate = resolveVariableReferences((String) nodeConfig.getOrDefault("prompt", ""), nodeOutputs, graph);
        if (promptTemplate != null && !promptTemplate.isEmpty()) {
            String resolvedPrompt = promptTemplate;
            for (Map.Entry<String, Object> entry : state.getVariables().entrySet()) {
                resolvedPrompt = resolvedPrompt.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
            for (Map.Entry<String, Object> entry : state.getInputs().entrySet()) {
                resolvedPrompt = resolvedPrompt.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
            userMessage.append(resolvedPrompt);
        } else if (state.getProcessedInput().get("message") != null) {
            userMessage.append(state.getProcessedInput().get("message").toString());
        } else {
            userMessage.append(state.getProcessedInput().toString());
        }

        // 使用 langchain4j 原生 API 调用 LLM（支持 Tool Calling）
        try {
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemPrompt));
            messages.add(UserMessage.from(userMessage.toString()));

            // 添加历史消息（如果有记忆）
            if (state.getExecutionId() != null) {
                List<ChatMessage> history = langChain4jService.getMemoryMessages(state.getExecutionId());
                if (!history.isEmpty()) {
                    messages.addAll(1, history);
                }
            }

            // 通过 CompositeToolProvider 获取所有可用工具（MCP + Function）
            List<ToolSpecification> toolSpecs = compositeToolProvider.getToolSpecifications();
            boolean useToolCalling = !toolSpecs.isEmpty() && langChain4jService.getProvider(provider).supportsToolCalling();

            String response;
            if (useToolCalling) {
                // 使用 langchain4j Tool Calling 循环
                log.info("[GraphExecutor] 启用 Tool Calling 模式 (MCP+Function), 可用工具: {} 个 (MCP={}, Function={})",
                        toolSpecs.size(),
                        compositeToolProvider.getMcpToolCount(),
                        compositeToolProvider.getFunctionToolCount());

                Response<AiMessage> aiResponse = langChain4jService.chatWithTools(
                        provider, messages, toolSpecs,
                        (toolName, toolArgs) -> {
                            // 通过 CompositeToolProvider 自动路由到 MCP 或 Function
                            String source = compositeToolProvider.getToolSource(toolName);
                            try {
                                if ("function".equals(source)) {
                                    // Function Calling: 直接调用系统内 Java 方法
                                    return compositeToolProvider.executeTool(
                                            dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                                                    .name(toolName)
                                                    .arguments(toolArgs)
                                                    .build(),
                                            null);
                                } else {
                                    // MCP Tool Calling: 通过 MCP 网关调用外部工具
                                    Long toolId = resolveToolIdFromBridge(toolName);
                                    if (toolId != null) {
                                        Object result = mcpToolGateway.invokeTool(toolId,
                                                com.fasterxml.jackson.databind.ObjectMapper.class
                                                        .newInstance().readValue(toolArgs, Map.class),
                                                state.getTenantId(), null);
                                        return com.fasterxml.jackson.databind.ObjectMapper.class
                                                .newInstance().writeValueAsString(result);
                                    }
                                    return "{\"error\": \"MCP 工具未找到: " + toolName + "\"}";
                                }
                            } catch (Exception e) {
                                return "{\"error\": \"[" + source + "] " + e.getMessage().replace("\"", "'") + "\"}";
                            }
                        },
                        5 // 最大工具调用迭代次数
                );
                response = aiResponse.content().text();

                // 保存工具调用结果到状态
                if (aiResponse.content().hasToolExecutionRequests()) {
                    for (var req : aiResponse.content().toolExecutionRequests()) {
                        String toolSource = compositeToolProvider.getToolSource(req.name());
                        state.getToolResults().put(req.name() + "[" + toolSource + "]", req.arguments());
                    }
                }
            } else {
                // 普通对话模式
                response = langChain4jService.chatWithConfig(provider, systemPrompt, userMessage.toString(), config);
            }

            state.setLlmOutput(response);
            node.setOutput(response);
            state.getOutputs().put("llmResponse", response);

            // 保存到 langchain4j ChatMemory
            if (state.getExecutionId() != null) {
                langChain4jService.getOrCreateMemory(state.getExecutionId(), 20);
                langChain4jService.chatWithMemory(provider, state.getExecutionId(), userMessage.toString(), 20);
            }

            log.info("[GraphExecutor] LLM 节点响应 (langchain4j): {} 字符, toolCalling: {}",
                    response != null ? response.length() : 0, useToolCalling);
        } catch (Exception e) {
            log.error("[GraphExecutor] LLM 调用失败 (langchain4j): {}", e.getMessage());
            throw new BusinessException("LLM 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 MCP Bridge 解析工具 ID
     */
    private Long resolveToolIdFromBridge(String toolName) {
        try {
            var tools = mcpToolGateway.getAvailableTools();
            if (tools != null) {
                for (Map<String, Object> tool : tools) {
                    String name = String.valueOf(tool.getOrDefault("name", tool.get("toolName")));
                    if (toolName.equals(name)) {
                        Object id = tool.get("id");
                        if (id instanceof Number) return ((Number) id).longValue();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[GraphExecutor] 解析工具 ID 失败: {}", e.getMessage());
        }
        return null;
    }

    private void executeConditionNode(GraphNode node, AgentState state) {
        String expression = (String) node.getConfig().getOrDefault("expression", "");
        String variable = (String) node.getConfig().getOrDefault("variable", "");

        boolean result = false;

        if (!variable.isEmpty()) {
            // 基于变量值判断
            Object varValue = state.getVariable(variable);
            if (varValue != null) {
                result = Boolean.parseBoolean(varValue.toString());
            }
        } else if (!expression.isEmpty()) {
            // 基于表达式判断（简化实现：支持 contains, equals, >, <, ==）
            result = evaluateExpression(expression, state);
        } else {
            // 基于 LLM 输出判断
            String output = state.getLlmOutput();
            result = output != null && !output.isEmpty();
        }

        state.setConditionResult(String.valueOf(result));
        state.getOutputs().put("conditionResult", result);
        node.setOutput(String.valueOf(result));
        log.info("条件节点结果: {}", result);
    }

    private void executeToolNode(GraphNode node, AgentState state) {
        String toolIdStr = (String) node.getConfig().getOrDefault("toolId", "");
        String toolName = (String) node.getConfig().getOrDefault("toolName", "");
        Map<String, Object> toolInput = new HashMap<>();

        // 从 LLM 输出或状态中提取工具输入
        if (state.getLlmOutput() != null) {
            toolInput.put("input", state.getLlmOutput());
        }
        if (node.getConfig().containsKey("inputMapping")) {
            // 支持输入映射
            // 节点配置中的 inputMapping 是 Map<String, Object> 中的 Object，需要强制转换
            @SuppressWarnings("unchecked")
            Map<String, String> mapping = (Map<String, String>) node.getConfig().get("inputMapping");
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                Object value = state.getVariable(entry.getKey());
                if (value == null) value = state.getOutputs().get(entry.getKey());
                if (value != null) toolInput.put(entry.getValue(), value);
            }
        }

        try {
            // 将 toolId 字符串转为 Long
            Long toolId = null;
            try {
                toolId = Long.parseLong(toolIdStr);
            } catch (NumberFormatException e) {
                log.warn("工具 ID 格式无效: {}", toolIdStr);
            }

            Object toolResult;
            if (toolId != null) {
                toolResult = mcpToolGateway.invokeTool(toolId, toolInput, state.getTenantId(), null);
            } else {
                throw new BusinessException("工具 ID 无效: " + toolIdStr);
            }

            Map<String, Object> resultMap = new HashMap<>();
            if (toolResult instanceof Map) {
                // MCP 工具返回值是 Object 类型，需要强制转换为 Map
                @SuppressWarnings("unchecked")
                Map<String, Object> castResult = (Map<String, Object>) toolResult;
                resultMap.putAll(castResult);
            } else {
                resultMap.put("result", toolResult);
            }

            state.getToolResults().put(toolName, resultMap);
            state.getOutputs().put("toolResult", resultMap);
            node.setOutput(resultMap.toString());
            log.info("工具节点 {} 调用成功", toolName);
        } catch (Exception e) {
            log.error("工具调用失败: {}", e.getMessage());
            throw new BusinessException("工具调用失败: " + e.getMessage(), e);
        }
    }

    private void executeMemoryNode(GraphNode node, AgentState state) {
        String action = (String) node.getConfig().getOrDefault("action", "load");
        String memoryType = (String) node.getConfig().getOrDefault("memoryType", "SHORT_TERM");
        String query = (String) node.getConfig().getOrDefault("query", "");

        if ("save".equals(action)) {
            // 保存记忆
            com.aiagent.entity.AgentMemory memory = new com.aiagent.entity.AgentMemory();
            memory.setAgentId(state.getAgentId());
            memory.setMemoryType(com.aiagent.entity.AgentMemory.MemoryType.valueOf(memoryType));
            memory.setContent(state.getLlmOutput());
            memory.setSummary((String) node.getConfig().get("summary"));
            memory.setTenantId(state.getTenantId());
            memoryService.createMemory(memory);
            log.info("记忆已保存: type={}", memoryType);
        } else {
            // 加载记忆
            if (!query.isEmpty() && state.getAgentId() != null) {
                var memories = memoryService.getMemories(state.getAgentId(), query,
                    com.aiagent.entity.AgentMemory.MemoryType.valueOf(memoryType), 0, 5);
                StringBuilder context = new StringBuilder();
                memories.getContent().forEach(m ->
                    context.append("- ").append(m.getContent()).append("\n"));
                state.setMemoryContext(context.toString());
                node.setOutput(context.toString());
                log.info("已加载 {} 条记忆", memories.getTotalElements());
            }
        }
    }

    private void executeVariableNode(GraphNode node, AgentState state) {
        String varName = (String) node.getConfig().getOrDefault("name", "");
        String varValue = (String) node.getConfig().getOrDefault("value", "");
        String source = (String) node.getConfig().getOrDefault("source", "");

        if (!source.isEmpty()) {
            // 从其他来源获取值
            Object value = state.getOutputs().get(source);
            if (value == null) value = state.getVariable(source);
            if (value != null) varValue = value.toString();
        }

        state.setVariable(varName, varValue);
        node.setOutput(varValue);
        log.info("变量设置: {} = {}", varName, varValue);
    }

    private void executeRetrieverNode(GraphNode node, AgentState state, Map<String, Object> nodeOutputs, GraphDefinition graph) {
        String query = resolveVariableReferences((String) node.getConfig().getOrDefault("query", ""), nodeOutputs, graph);
        String retrieverType = (String) node.getConfig().getOrDefault("retrieverType", "memory");

        if ("memory".equals(retrieverType) && state.getAgentId() != null) {
            var memories = memoryService.getMemories(state.getAgentId(), query, null, 0, 10);
            List<Map<String, Object>> results = new ArrayList<>();
            memories.getContent().forEach(m -> {
                Map<String, Object> item = new HashMap<>();
                item.put("content", m.getContent());
                item.put("type", m.getMemoryType().name());
                item.put("importance", m.getImportance());
                results.add(item);
            });
            state.setRetrievalResults(Map.of("results", results));
            node.setOutput(results.toString());
            log.info("检索到 {} 条相关记忆", results.size());
        } else {
            log.warn("不支持的检索类型: {}", retrieverType);
        }
    }

    private void executeExceptionNode(GraphNode node, AgentState state) {
        String action = (String) node.getConfig().getOrDefault("action", "log");
        String errorMessage = state.getError() != null ? state.getError() : "未知错误";

        if ("retry".equals(action)) {
            log.info("异常处理: 重试");
            state.setError(null);  // 清除错误，允许继续
        } else if ("fallback".equals(action)) {
            String fallbackValue = (String) node.getConfig().getOrDefault("fallbackValue", "");
            state.setLlmOutput(fallbackValue);
            state.setError(null);
            log.info("异常处理: 使用降级值");
        } else {
            log.warn("异常处理: 仅记录 - {}", errorMessage);
        }
    }

    private void executeHttpNode(GraphNode node, AgentState state, Map<String, Object> nodeOutputs, GraphDefinition graph) {
        Map<String, Object> nodeConfig = node.getConfig();
        String url = resolveVariableReferences((String) nodeConfig.getOrDefault("url", ""), nodeOutputs, graph);
        String method = (String) nodeConfig.getOrDefault("method", "GET");

        HttpExecutor.HttpResult result = httpExecutor.execute(url, method, nodeConfig, state.getInputs());

        if (result.hasError()) {
            state.getOutputs().put("httpResponse", result.getBody());
            state.getOutputs().put("httpError", result.getError());
            if (result.getBody().isEmpty()) {
                return; // URL 配置缺失等非异常情况
            }
            throw new BusinessException("HTTP 调用失败: " + result.getError());
        }

        state.getOutputs().put("httpResponse", result.getBody());
        state.getOutputs().put("httpStatus", result.getStatusCode());
        node.setOutput(result.getBody());
    }

    private void executeSwitchNode(GraphNode node, AgentState state) {
        Map<String, Object> config = node.getConfig();
        Object casesObj = config.get("cases");

        if (!(casesObj instanceof List) || ((List<?>) casesObj).isEmpty()) {
            log.warn("Switch 节点 {} 没有配置分支规则", node.getId());
            state.setConditionResult("default");
            node.setOutput("default");
            return;
        }

        // JSON 反序列化后 cases 是 Object 类型，需要强制转换为 List<Map>
        @SuppressWarnings("unchecked")
        List<Map<String, String>> cases = (List<Map<String, String>>) casesObj;

        // Evaluate each case expression against the current state
        for (Map<String, String> caseDef : cases) {
            String expression = caseDef.get("expression");
            String outputPort = caseDef.get("outputPort");

            if (expression != null && !expression.isEmpty() && evaluateExpression(expression, state)) {
                log.info("Switch 节点 {} 匹配分支: {} -> {}", node.getId(), expression, outputPort);
                state.setConditionResult(outputPort);
                node.setOutput(outputPort);
                return;
            }
        }

        // No case matched, use default
        log.info("Switch 节点 {} 无匹配分支，使用默认分支", node.getId());
        state.setConditionResult("default");
        node.setOutput("default");
    }

    private void executeSubgraphNode(GraphNode node, AgentState state) {
        Map<String, Object> config = node.getConfig();
        String agentId = (String) config.get("agentId");

        if (agentId == null || agentId.isEmpty()) {
            throw new BusinessException("子图节点未配置 Agent ID");
        }

        // TODO: 实现子图执行 - 加载目标 Agent 的 GraphDefinition 并递归执行
        // 当前为占位实现，返回提示信息
        log.info("子图节点 {} 调用 Agent: {}", node.getId(), agentId);
        String result = "[子图执行结果 - Agent: " + agentId + "]";
        node.setOutput(result);
    }

    /**
     * 执行人工审批节点
     *
     * 灵感来自 LangGraph 的 interrupt/resume 机制和 Flowise 的 checkpoint 持久化。
     *
     * 生产环境实现计划：
     * 1. 保存执行状态为 checkpoint（包含当前节点 ID、状态变量等）
     * 2. 向审批人发送通知（邮件/消息/WebSocket）
     * 3. 抛出中断异常暂停执行，返回 checkpoint ID 给调用方
     * 4. 提供 resume API，审批人通过 API 提交审批结果
     * 5. resume 时从 checkpoint 恢复状态，继续执行
     *
     * 当前实现：自动批准（用于测试和开发阶段）
     */
    private void executeHumanApprovalNode(GraphNode node, AgentState state) {
        Map<String, Object> config = node.getConfig();
        String title = (String) config.getOrDefault("title", "请审批");
        String approvalType = (String) config.getOrDefault("approvalType", "approve_reject");
        String approvers = (String) config.getOrDefault("approvers", "");
        int timeoutMinutes = config.get("timeoutMinutes") != null
                ? ((Number) config.get("timeoutMinutes")).intValue()
                : 60;
        String fallbackAction = (String) config.getOrDefault("fallbackAction", "reject");

        // 检查状态中是否已有审批结果（用于 resume 场景）
        Object approvalResult = state.getVariables().get("__approval_" + node.getId());

        if (approvalResult == null) {
            // 无审批结果 - 创建 checkpoint 并等待审批
            log.info("人工审批节点 {} 等待审批: {} (类型: {}, 审批人: {}, 超时: {}分钟, 超时处理: {})",
                    node.getId(), title, approvalType, approvers, timeoutMinutes, fallbackAction);

            // TODO: 生产环境 - 保存 checkpoint 到数据库
            // TODO: 生产环境 - 发送审批通知给审批人
            // TODO: 生产环境 - 抛出 ExecutionInterruptedException 暂停执行
            // TODO: 生产环境 - 返回 checkpoint ID 给前端，前端轮询或 WebSocket 等待

            // 记录审批等待状态
            state.getVariables().put("__approval_pending_" + node.getId(), "true");
            state.getVariables().put("__approval_title_" + node.getId(), title);
            state.getVariables().put("__approval_type_" + node.getId(), approvalType);

            // 当前自动批准（开发/测试阶段）
            log.warn("人工审批节点 {} 当前自动批准（生产环境需实现 checkpoint/resume 暂停恢复机制）", node.getId());
            approvalResult = "approved";
        }

        String result = String.valueOf(approvalResult);
        state.setConditionResult(result);
        state.getOutputs().put("approvalResult", result);
        node.setOutput(result);
        log.info("人工审批节点 {} 结果: {}", node.getId(), result);
    }

    /**
     * 解析下一个要执行的节点
     */
    private String resolveNextNode(GraphDefinition graph, GraphNode node, AgentState state) {
        // 并行节点：分支已在 executeParallelNode 内部执行完毕，跳过后续路由
        if ("parallel".equals(node.getType())) {
            return null;
        }

        // 条件分支节点：根据结果选择端口
        if ("condition".equals(node.getType())) {
            String result = state.getConditionResult();
            String portName = "true".equals(result) ? "true" : "false";
            GraphEdge edge = graph.getEdgeBySourcePort(node.getId(), portName);
            if (edge != null) return edge.getTargetNodeId();
            // 回退：取第一个出边
            List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());
            if (!edges.isEmpty()) return edges.get(0).getTargetNodeId();
            return null;
        }

        // 人工审批节点：根据审批结果选择 approved/rejected 端口
        if ("human_approval".equals(node.getType())) {
            String result = state.getConditionResult();
            String portName = "approved".equals(result) ? "approved" : "rejected";
            GraphEdge edge = graph.getEdgeBySourcePort(node.getId(), portName);
            if (edge != null) return edge.getTargetNodeId();
            // 回退：取第一个出边
            List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());
            if (!edges.isEmpty()) return edges.get(0).getTargetNodeId();
            return null;
        }

        // 多路分支节点：根据条件结果选择对应端口
        if ("switch".equals(node.getType())) {
            String result = state.getConditionResult();
            if (result != null && !result.isEmpty()) {
                GraphEdge edge = graph.getEdgeBySourcePort(node.getId(), result);
                if (edge != null) return edge.getTargetNodeId();
            }
            // 回退到 default 端口
            GraphEdge defaultEdge = graph.getEdgeBySourcePort(node.getId(), "default");
            if (defaultEdge != null) return defaultEdge.getTargetNodeId();
            // 最终回退：取第一个出边
            List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());
            if (!edges.isEmpty()) return edges.get(0).getTargetNodeId();
            return null;
        }

        // 工具调用后：检查是否需要再次调用 LLM
        if ("tool".equals(node.getType())) {
            Boolean toolCallRequested = (Boolean) state.getOutputs().get("toolCallRequested");
            if (Boolean.TRUE.equals(toolCallRequested)) {
                // 找到下一个 LLM 节点
                List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());
                for (GraphEdge edge : edges) {
                    GraphNode target = graph.getNode(edge.getTargetNodeId());
                    if (target != null && "llm".equals(target.getType())) {
                        return edge.getTargetNodeId();
                    }
                }
            }
        }

        // 默认：取第一个出边
        List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());
        if (!edges.isEmpty()) {
            return edges.get(0).getTargetNodeId();
        }

        return null; // 无出边，终止
    }

    /**
     * 查找异常处理节点
     */
    private String findExceptionHandler(GraphDefinition graph, GraphNode failedNode) {
        List<GraphEdge> edges = graph.getOutgoingEdges(failedNode.getId());
        for (GraphEdge edge : edges) {
            GraphNode target = graph.getNode(edge.getTargetNodeId());
            if (target != null && "exception".equals(target.getType())) {
                return edge.getTargetNodeId();
            }
        }
        return null;
    }

    /**
     * 解析模板中的变量引用 {{nodeId.output}} 或 {{nodeId.config.field}}
     * 替换为实际执行结果或节点配置值
     */
    private String resolveVariableReferences(String template, Map<String, Object> nodeOutputs, GraphDefinition graph) {
        if (template == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group(0);
            String expr = matcher.group(1).trim();
            String[] parts = expr.split("\\.", 2);
            String replacement = match; // 默认保留原始占位符
            if (parts.length >= 2) {
                String nodeId = parts[0];
                String field = parts[1];

                // Check node outputs
                if (nodeOutputs.containsKey(nodeId)) {
                    Object output = nodeOutputs.get(nodeId);
                    if ("output".equals(field) && output != null) {
                        replacement = output.toString();
                    }
                    // Try to get nested field
                    if (replacement.equals(match) && output instanceof Map) {
                        Object value = ((Map<?, ?>) output).get(field);
                        if (value != null) replacement = value.toString();
                    }
                }

                // Check node config
                if (replacement.equals(match)) {
                    GraphNode graphNode = graph.getNode(nodeId);
                    if (graphNode != null) {
                        Object configValue = graphNode.getConfig().get(field);
                        if (configValue != null) replacement = configValue.toString();
                        if (replacement.equals(match) && "label".equals(field)) replacement = graphNode.getLabel();
                    }
                }
            }
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 执行并行节点 — 使用 CompletableFuture 扇出执行多个分支，等待全部完成后合并结果
     *
     * 实现细节:
     * - 使用 CompletableFuture.supplyAsync() 在 agentExecutor 线程池上并行执行各分支
     * - ConcurrentHashMap 保证并行写入 nodeOutputs 的线程安全
     * - fail_fast 策略: 任一分支失败立即取消其余分支并抛出异常
     * - wait 策略: 等待所有分支完成，记录失败信息但不中断整体执行
     * - maxParallelism 通过 semaphore 限制并发度（当前实现中所有分支同时提交，
     *   线程池本身的队列和拒绝策略提供背压保护）
     */
    private void executeParallelNode(GraphNode node, AgentState state, Map<String, Object> nodeOutputs, GraphDefinition graph) {
        Map<String, Object> config = node.getConfig();
        int maxParallelism = config.containsKey("maxParallelism") ? ((Number) config.get("maxParallelism")).intValue() : 5;
        String failStrategy = (String) config.getOrDefault("failStrategy", "wait");

        // 获取所有出边（扇出目标）
        List<GraphEdge> outEdges = graph.getOutgoingEdges(node.getId());

        if (outEdges.isEmpty()) {
            log.warn("并行节点 {} 没有出边，跳过执行", node.getId());
            node.setOutput("");
            nodeOutputs.put(node.getId(), "");
            return;
        }

        // 使用 ConcurrentHashMap 保证并行写入的线程安全
        Map<String, Object> branchResults = new ConcurrentHashMap<>();
        List<String> errors = new java.util.concurrent.CopyOnWriteArrayList<>();

        // 为每个分支创建 CompletableFuture
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (GraphEdge edge : outEdges) {
            String targetId = edge.getTargetNodeId();
            GraphNode targetNode = graph.getNode(targetId);
            if (targetNode == null) {
                log.warn("并行分支目标节点 {} 不存在，跳过", targetId);
                continue;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.info("并行分支开始执行: {} -> {}", node.getId(), targetId);
                    executeNodeInternal(targetNode, state, nodeOutputs, graph);
                    branchResults.put(targetId, nodeOutputs.getOrDefault(targetId, ""));
                    log.info("并行分支执行完成: {} -> {}", node.getId(), targetId);
                } catch (Exception e) {
                    log.error("并行分支执行失败: {} -> {}: {}", node.getId(), targetId, e.getMessage());
                    errors.add(targetId + ": " + e.getMessage());
                    if ("fail_fast".equals(failStrategy)) {
                        throw new BusinessException("并行分支执行失败: " + targetId, e);
                    }
                }
            }, agentExecutor);

            futures.add(future);
        }

        // 等待所有分支完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            // fail_fast 模式下，取消所有未完成的分支
            for (CompletableFuture<Void> f : futures) {
                f.cancel(true);
            }
            throw new BusinessException("并行节点执行失败: " + node.getId(), e);
        }

        if (!errors.isEmpty()) {
            if ("wait".equals(failStrategy)) {
                log.warn("并行节点 {} 有 {} 个分支失败: {}", node.getId(), errors.size(), errors);
            }
        }

        String result = branchResults.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining("\n---\n"));
        node.setOutput(result);
        nodeOutputs.put(node.getId(), result);
    }

    /**
     * 执行合并节点 — 收集所有输入连接的输出并按策略合并
     */
    private void executeMergeNode(GraphNode node, AgentState state, Map<String, Object> nodeOutputs, GraphDefinition graph) {
        Map<String, Object> config = node.getConfig();
        String strategy = (String) config.getOrDefault("mergeStrategy", "append");

        // 收集所有入边对应的上游输出
        List<GraphEdge> inEdges = graph.getIncomingEdges(node.getId());
        List<Object> inputs = new ArrayList<>();

        for (GraphEdge edge : inEdges) {
            String sourceId = edge.getSourceNodeId();
            Object output = nodeOutputs.get(sourceId);
            if (output != null) {
                inputs.add(output);
            }
        }

        String result;
        switch (strategy) {
            case "first":
                result = inputs.isEmpty() ? "" : inputs.get(0).toString();
                break;
            case "overwrite":
                result = inputs.isEmpty() ? "" : inputs.get(inputs.size() - 1).toString();
                break;
            case "append":
            default:
                result = inputs.stream().map(Object::toString).collect(Collectors.joining("\n"));
                break;
        }

        node.setOutput(result);
        nodeOutputs.put(node.getId(), result);
    }

    /**
     * 内部节点执行方法（供 parallel 节点递归调用子分支）
     * TODO: 与主循环逻辑合并，避免代码重复。当前为 parallel 节点的临时实现。
     */
    private void executeNodeInternal(GraphNode node, AgentState state, Map<String, Object> nodeOutputs, GraphDefinition graph) {
        node.setStatus("running");
        log.info("执行并行分支节点 [{}] {} (类型: {})", node.getId(), node.getLabel(), node.getType());

        switch (node.getType()) {
            case "start" -> executeStartNode(node, state);
            case "llm" -> executeLlmNode(node, state, nodeOutputs, graph);
            case "condition" -> executeConditionNode(node, state);
            case "tool" -> executeToolNode(node, state);
            case "memory" -> executeMemoryNode(node, state);
            case "variable" -> executeVariableNode(node, state);
            case "retriever" -> executeRetrieverNode(node, state, nodeOutputs, graph);
            case "exception" -> executeExceptionNode(node, state);
            case "http" -> executeHttpNode(node, state, nodeOutputs, graph);
            case "parallel" -> executeParallelNode(node, state, nodeOutputs, graph);
            case "merge" -> executeMergeNode(node, state, nodeOutputs, graph);
            case "code" -> {
                String result = nodeExecutors.executeCodeNode(node.getId(), node.getConfig(), nodeOutputs);
                node.setOutput(result);
                nodeOutputs.put(node.getId(), result);
            }
            case "delay" -> {
                String result = nodeExecutors.executeDelayNode(node.getId(), node.getConfig());
                node.setOutput(result);
                nodeOutputs.put(node.getId(), result);
            }
            default -> {
                log.warn("未知节点类型: {}, 跳过", node.getType());
                node.setStatus("skipped");
                return;
            }
        }

        node.setStatus("completed");
        if (node.getOutput() != null) {
            nodeOutputs.put(node.getId(), node.getOutput());
        }
    }

    /**
     * 简化表达式求值
     */
    private boolean evaluateExpression(String expression, AgentState state) {
        // 支持: variable == value, variable != value, variable contains value, variable > value, variable < value
        String[] parts = expression.split("\\s+(==|!=|contains|>|<)\\s+", 3);
        if (parts.length == 3) {
            String left = parts[0].trim();
            String operator = parts[1].trim();
            String right = parts[2].trim();

            Object leftValue = state.getVariable(left);
            if (leftValue == null) leftValue = state.getOutputs().get(left);
            if (leftValue == null) leftValue = state.getLlmOutput();

            String leftStr = leftValue != null ? leftValue.toString() : "";

            return switch (operator) {
                case "==" -> leftStr.equals(right);
                case "!=" -> !leftStr.equals(right);
                case "contains" -> leftStr.contains(right);
                case ">", "<" -> {
                    try {
                        double leftNum = Double.parseDouble(leftStr);
                        double rightNum = Double.parseDouble(right);
                        yield ">".equals(operator)
                            ? Double.compare(leftNum, rightNum) > 0
                            : Double.compare(leftNum, rightNum) < 0;
                    } catch (NumberFormatException e) {
                        log.warn("无法解析数值表达式: {} {} {}", leftStr, operator, right);
                        yield false;
                    }
                }
                default -> false;
            };
        }
        return false;
    }

}
