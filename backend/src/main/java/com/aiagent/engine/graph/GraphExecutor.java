package com.aiagent.engine.graph;

import com.aiagent.exception.BusinessException;
import com.aiagent.mcp.McpToolGateway;
import com.aiagent.service.MemoryService;
import com.aiagent.service.llm.LangChain4jService;
import com.aiagent.service.llm.LlmProviderConfig;
import com.aiagent.service.tool.CompositeToolProvider;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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
@RequiredArgsConstructor
public class GraphExecutor {

    private final LangChain4jService langChain4jService;
    private final CompositeToolProvider compositeToolProvider;
    private final McpToolGateway mcpToolGateway;
    private final MemoryService memoryService;

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

            try {
                log.info("执行节点 [{}] {} (类型: {}, 第 {} 步)",
                    node.getId(), node.getLabel(), node.getType(), node.getExecutionOrder());

                // 根据节点类型执行
                switch (node.getType()) {
                    case "start" -> executeStartNode(node, state);
                    case "llm" -> executeLlmNode(node, state);
                    case "condition" -> executeConditionNode(node, state);
                    case "tool" -> executeToolNode(node, state);
                    case "memory" -> executeMemoryNode(node, state);
                    case "variable" -> executeVariableNode(node, state);
                    case "retriever" -> executeRetrieverNode(node, state);
                    case "exception" -> executeExceptionNode(node, state);
                    case "http" -> executeHttpNode(node, state);
                    case "end" -> { node.setStatus("completed"); state.setShouldTerminate(true); break; }
                    default -> {
                        log.warn("未知节点类型: {}, 跳过", node.getType());
                        node.setStatus("skipped");
                    }
                }

                if (!state.isShouldTerminate()) {
                    node.setStatus("completed");
                    // 决定下一个节点
                    currentNodeId = resolveNextNode(graph, node, state);
                }

            } catch (Exception e) {
                log.error("节点 {} 执行失败: {}", node.getId(), e.getMessage(), e);
                node.setStatus("failed");
                node.setError(e.getMessage());

                // 查找异常处理节点
                currentNodeId = findExceptionHandler(graph, node);
                if (currentNodeId == null) {
                    state.setError("节点 " + node.getLabel() + " 执行失败: " + e.getMessage());
                    break;
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

    private void executeLlmNode(GraphNode node, AgentState state) {
        Map<String, Object> nodeConfig = node.getConfig();
        String provider = (String) nodeConfig.getOrDefault("provider", "openai");
        String modelName = (String) nodeConfig.getOrDefault("model", null);
        String systemPrompt = (String) nodeConfig.getOrDefault("systemPrompt", "你是一个 AI 助手。");

        // 构建 langchain4j 配置
        LlmProviderConfig config = LlmProviderConfig.builder()
                .provider(provider)
                .modelName(modelName)
                .temperature(nodeConfig.get("temperature") != null ? ((Number) nodeConfig.get("temperature")).doubleValue() : 0.7)
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
        String promptTemplate = (String) nodeConfig.getOrDefault("prompt", "");
        if (!promptTemplate.isEmpty()) {
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
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
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
                throw new RuntimeException("工具 ID 无效: " + toolIdStr);
            }

            Map<String, Object> resultMap = new HashMap<>();
            if (toolResult instanceof Map) {
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
            throw new RuntimeException("工具调用失败: " + e.getMessage(), e);
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

    private void executeRetrieverNode(GraphNode node, AgentState state) {
        String query = (String) node.getConfig().getOrDefault("query", "");
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

    private void executeHttpNode(GraphNode node, AgentState state) {
        Map<String, Object> nodeConfig = node.getConfig();
        String url = (String) nodeConfig.getOrDefault("url", "");
        String method = (String) nodeConfig.getOrDefault("method", "GET");
        log.info("HTTP 节点: {} {}", method, url);

        if (url == null || url.isBlank()) {
            log.error("HTTP 节点缺少 url 配置");
            state.getOutputs().put("httpResponse", "");
            state.getOutputs().put("httpError", "Missing URL configuration");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory reqFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
            reqFactory.setConnectTimeout(10_000);
            reqFactory.setReadTimeout(30_000);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (nodeConfig.containsKey("headers")) {
                @SuppressWarnings("unchecked")
                Map<String, String> headerMap = (Map<String, String>) nodeConfig.get("headers");
                headerMap.forEach(headers::set);
            }

            // 构建请求体
            Object body = null;
            if (nodeConfig.containsKey("body")) {
                body = nodeConfig.get("body");
            } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                body = state.getInputs();
            }

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

            log.info("HTTP 节点发送请求: {} {}", httpMethod, url);
            ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);

            String responseBody = response.getBody();
            log.info("HTTP 节点响应: status={}, bodyLength={}", response.getStatusCode(), responseBody != null ? responseBody.length() : 0);

            state.getOutputs().put("httpResponse", responseBody != null ? responseBody : "");
            state.getOutputs().put("httpStatus", response.getStatusCode().value());
            node.setOutput(responseBody != null ? responseBody : "");

        } catch (Exception e) {
            log.error("HTTP 节点调用失败: {} {} - {}", method, url, e.getMessage());
            state.getOutputs().put("httpResponse", "");
            state.getOutputs().put("httpError", e.getMessage());
            throw new RuntimeException("HTTP 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析下一个要执行的节点
     */
    private String resolveNextNode(GraphDefinition graph, GraphNode node, AgentState state) {
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
                case ">" -> Double.compare(Double.parseDouble(leftStr), Double.parseDouble(right)) > 0;
                case "<" -> Double.compare(Double.parseDouble(leftStr), Double.parseDouble(right)) < 0;
                default -> false;
            };
        }
        return false;
    }
}
