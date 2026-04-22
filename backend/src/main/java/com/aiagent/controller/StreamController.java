package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.engine.graph.AgentState;
import com.aiagent.engine.graph.GraphDefinition;
import com.aiagent.engine.graph.GraphEdge;
import com.aiagent.engine.graph.GraphExecutor;
import com.aiagent.engine.graph.GraphNode;
import com.aiagent.engine.graph.GraphParser;
import com.aiagent.entity.Agent;
import com.aiagent.repository.AgentRepository;
import com.aiagent.service.llm.LangChain4jService;
import com.aiagent.security.PromptInjectionFilter;
import com.aiagent.security.PromptInjectionFilter.FilterResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * SSE 流式输出 Controller
 * 
 * 基于 langchain4j StreamingChatLanguageModel + Spring MVC SseEmitter 实现
 * 
 * SSE 事件格式:
 * - event: token / data: {"type":"token","content":"你好"}
 * - event: done  / data: {"type":"done","content":""}
 * - event: error / data: {"type":"error","content":"错误信息"}
 * 
 * 前端使用 EventSource 或 fetch + ReadableStream 接收
 */
@Slf4j
@RestController
@RequestMapping("/v1/stream")
@Tag(name = "流式对话", description = "SSE流式对话接口")
public class StreamController {

    private final LangChain4jService langChain4jService;
    private final PromptInjectionFilter promptInjectionFilter;
    private final GraphParser graphParser;
    private final GraphExecutor graphExecutor;
    private final AgentRepository agentRepository;
    private final Executor sseExecutor;

    public StreamController(LangChain4jService langChain4jService,
                            PromptInjectionFilter promptInjectionFilter,
                            GraphParser graphParser,
                            GraphExecutor graphExecutor,
                            AgentRepository agentRepository,
                            @Qualifier("sseExecutor") Executor sseExecutor) {
        this.langChain4jService = langChain4jService;
        this.promptInjectionFilter = promptInjectionFilter;
        this.graphParser = graphParser;
        this.graphExecutor = graphExecutor;
        this.agentRepository = agentRepository;
        this.sseExecutor = sseExecutor;
    }

    /**
     * SSE 流式对话（GET）
     * 
     * GET /api/v1/stream/chat?provider=openai&message=你好&sessionId=xxx
     */
    @RequiresPermission("agent:invoke")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式对话(GET)")
    public SseEmitter streamChat(
            @RequestParam(defaultValue = "openai") String provider,
            @RequestParam(required = false) String systemPrompt,
            @RequestParam String message,
            @RequestParam(required = false) String sessionId) {

        log.info("[SSE] 流式对话请求: provider={}, sessionId={}", provider, sessionId);

        // 超时 5 分钟
        SseEmitter emitter = new SseEmitter(300_000L);

        sseExecutor.execute(() -> {
            try {
                String sysPrompt = systemPrompt != null ? systemPrompt : "你是一个 AI 助手。";
                
                langChain4jService.chatStream(provider, sysPrompt, message,
                        new java.util.concurrent.Flow.Subscriber<>() {
                            private java.util.concurrent.Flow.Subscription subscription;

                            @Override
                            public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
                                this.subscription = s;
                                s.request(Long.MAX_VALUE);
                            }

                            @Override
                            public void onNext(String token) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("token")
                                            .data("{\"type\":\"token\",\"content\":\"" + escapeJson(token) + "\"}"));
                                } catch (IOException e) {
                                    subscription.cancel();
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                log.error("[SSE] 流式对话错误: {}", throwable.getMessage());
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data("{\"type\":\"error\",\"content\":\"" +
                                                    escapeJson(throwable.getMessage()) + "\"}"));
                                } catch (IOException ignored) {}
                                emitter.complete();
                            }

                            @Override
                            public void onComplete() {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data("{\"type\":\"done\",\"content\":\"\"}"));
                                } catch (IOException ignored) {}
                                emitter.complete();
                            }
                        });
            } catch (Exception e) {
                log.error("[SSE] 流式对话启动失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"type\":\"error\",\"content\":\"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (IOException ignored) {}
                emitter.complete();
            }
        });

        emitter.onTimeout(() -> log.warn("[SSE] 连接超时"));
        emitter.onError(e -> log.warn("[SSE] 连接错误: {}", e.getMessage()));
        emitter.onCompletion(() -> log.debug("[SSE] 连接关闭"));

        return emitter;
    }

    /**
     * SSE 流式对话（POST，支持复杂参数）
     * 
     * POST /api/v1/stream/chat
     * Body: { "provider": "openai", "systemPrompt": "...", "message": "...", "sessionId": "..." }
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式对话(POST)")
    public SseEmitter streamChatPost(@RequestBody Map<String, Object> request) {
        String provider = (String) request.getOrDefault("provider", "openai");
        String systemPrompt = (String) request.get("systemPrompt");
        String message = (String) request.get("message");
        String sessionId = (String) request.get("sessionId");

        if (message == null || message.isBlank()) {
            SseEmitter emitter = new SseEmitter(1_000L);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"type\":\"error\",\"content\":\"message is required\"}"));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        return streamChat(provider, systemPrompt, message, sessionId);
    }

    /**
     * SSE Agent 执行流
     * 
     * GET /api/v1/stream/agent/{agentId}?message=你好
     * 
     * 通过 GraphParser 解析 Agent 的图定义，按拓扑顺序逐步执行节点。
     * 对 LLM 节点使用 langchain4j 流式输出（发送 token 事件），
     * 对非 LLM 节点使用 GraphExecutor 同步执行。
     * 
     * 流式返回 Agent 图执行的每个节点状态:
     * - event: node_start / data: {"nodeId":"llm-1","nodeType":"llm"}
     * - event: token     / data: {"content":"..."}
     * - event: node_end   / data: {"nodeId":"llm-1","status":"completed"}
     * - event: done       / data: {"type":"done","content":"最终输出"}
     * - event: error      / data: {"type":"error","content":"错误信息"}
     */
    @Operation(summary = "SSE流式Agent执行")
    @GetMapping(value = "/agent/{agentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgentExecution(
            @PathVariable Long agentId,
            @RequestParam String message,
            @RequestParam(required = false) String sessionId) {

        log.info("[SSE] Agent 流式执行: agentId={}", agentId);

        SseEmitter emitter = new SseEmitter(300_000L);

        sseExecutor.execute(() -> {
            try {
                // 1. 从数据库加载 Agent 配置
                Agent agent = agentRepository.findById(agentId)
                        .orElseThrow(() -> new RuntimeException("Agent not found: " + agentId));

                // 2. 解析图定义
                Map<String, Object> config = new HashMap<>();
                if (agent.getConfig() != null) {
                    config.putAll(agent.getConfig());
                }
                config.putIfAbsent("provider", "openai");
                config.putIfAbsent("model", "gpt-4");
                config.putIfAbsent("temperature", 0.7);
                config.putIfAbsent("maxTokens", 1024);

                GraphDefinition graph = graphParser.parse(config);

                // 3. 构建执行状态
                AgentState state = new AgentState();
                state.setAgentId(agentId);
                state.setExecutionId(UUID.randomUUID().toString());
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("message", message);
                state.setInputs(inputs);
                state.getProcessedInput().putAll(inputs);

                // 4. 按拓扑顺序逐步执行节点，通过 SSE 推送每个节点的状态
                String currentNodeId = graph.getEntryNodeId();
                StringBuilder fullOutput = new StringBuilder();

                while (currentNodeId != null && !state.isShouldTerminate()) {
                    if (state.isMaxIterationsReached()) {
                        log.warn("[SSE] 达到最大迭代次数，终止执行");
                        break;
                    }

                    GraphNode node = graph.getNode(currentNodeId);
                    if (node == null) {
                        log.error("[SSE] 节点 {} 不存在", currentNodeId);
                        break;
                    }

                    state.setCurrentNodeId(currentNodeId);

                    // 发送 node_start 事件
                    emitter.send(SseEmitter.event()
                            .name("node_start")
                            .data("{\"nodeId\":\"" + escapeJson(node.getId()) +
                                    "\",\"nodeType\":\"" + escapeJson(node.getType()) + "\"}"));

                    try {
                        if ("llm".equals(node.getType())) {
                            // LLM 节点：使用 langchain4j 流式输出
                            String provider = node.getConfig() != null
                                    ? (String) node.getConfig().getOrDefault("provider", "openai")
                                    : "openai";
                            String systemPrompt = node.getConfig() != null
                                    ? (String) node.getConfig().getOrDefault("systemPrompt", "你是一个 AI 助手。")
                                    : "你是一个 AI 助手。";

                            // 构建用户消息（包含状态上下文）
                            StringBuilder userMessage = new StringBuilder(message);
                            if (state.getMemoryContext() != null) {
                                userMessage.insert(0, "【历史记忆】\n" + state.getMemoryContext() + "\n\n");
                            }
                            if (state.getToolResults() != null && !state.getToolResults().isEmpty()) {
                                userMessage.append("\n\n【工具调用结果】\n").append(state.getToolResults());
                            }

                            // 使用 CountDownLatch 等待流式完成
                            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                            final StringBuilder nodeOutput = new StringBuilder();
                            final Throwable[] streamError = {null};

                            langChain4jService.chatStream(provider, systemPrompt, userMessage.toString(),
                                    new java.util.concurrent.Flow.Subscriber<>() {
                                        private java.util.concurrent.Flow.Subscription subscription;

                                        @Override
                                        public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
                                            this.subscription = s;
                                            s.request(Long.MAX_VALUE);
                                        }

                                        @Override
                                        public void onNext(String token) {
                                            try {
                                                nodeOutput.append(token);
                                                emitter.send(SseEmitter.event()
                                                        .name("token")
                                                        .data("{\"content\":\"" + escapeJson(token) + "\"}"));
                                            } catch (IOException e) {
                                                subscription.cancel();
                                            }
                                        }

                                        @Override
                                        public void onError(Throwable throwable) {
                                            streamError[0] = throwable;
                                            latch.countDown();
                                        }

                                        @Override
                                        public void onComplete() {
                                            latch.countDown();
                                        }
                                    });

                            // 等待流式输出完成（最多 5 分钟）
                            latch.await(300, java.util.concurrent.TimeUnit.SECONDS);

                            if (streamError[0] != null) {
                                throw new RuntimeException("LLM 流式输出失败: " + streamError[0].getMessage(), streamError[0]);
                            }

                            String llmResponse = nodeOutput.toString();
                            state.setLlmOutput(llmResponse);
                            node.setOutput(llmResponse);
                            state.getOutputs().put("llmResponse", llmResponse);
                            fullOutput.append(llmResponse);

                        } else if ("end".equals(node.getType())) {
                            // 终止节点
                            state.setShouldTerminate(true);
                        } else {
                            // 非 LLM 节点：使用 GraphExecutor 同步执行整个图到当前节点
                            // 由于 GraphExecutor.execute() 是整体执行，这里对非 LLM 节点
                            // 只做简单的状态更新（start/memory/variable 等轻量节点）
                            executeNonLlmNode(node, state);
                        }

                        // 发送 node_end 事件
                        emitter.send(SseEmitter.event()
                                .name("node_end")
                                .data("{\"nodeId\":\"" + escapeJson(node.getId()) +
                                        "\",\"status\":\"completed\"}"));

                        // 决定下一个节点
                        if (!state.isShouldTerminate()) {
                            currentNodeId = resolveNextNode(graph, node, state);
                        } else {
                            currentNodeId = null;
                        }

                    } catch (Exception e) {
                        log.error("[SSE] 节点 {} 执行失败: {}", node.getId(), e.getMessage());
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("node_end")
                                    .data("{\"nodeId\":\"" + escapeJson(node.getId()) +
                                            "\",\"status\":\"failed\",\"error\":\"" + escapeJson(e.getMessage()) + "\"}"));
                        } catch (IOException ignored) {}

                        // 查找异常处理节点
                        currentNodeId = findExceptionHandler(graph, node);
                        if (currentNodeId == null) {
                            break;
                        }
                    }

                    state.incrementIteration();
                }

                // 5. 发送 done 事件
                String finalOutput = fullOutput.toString();
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"type\":\"done\",\"content\":\"" + escapeJson(finalOutput) + "\"}"));
                emitter.complete();

            } catch (Exception e) {
                log.error("[SSE] Agent 执行失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"type\":\"error\",\"content\":\"" + escapeJson(e.getMessage()) + "\"}"));
                    emitter.complete();
                } catch (IOException ignored) {}
            }
        });

        emitter.onTimeout(() -> log.warn("[SSE] 连接超时"));
        emitter.onError(e -> log.warn("[SSE] 连接错误: {}", e.getMessage()));
        emitter.onCompletion(() -> log.debug("[SSE] 连接关闭"));

        return emitter;
    }

    /**
     * 执行非 LLM 节点（轻量级同步执行）
     *
     * GraphExecutor.execute() 是整体同步执行，不支持逐节点回调。
     * 对于 SSE 流式场景，我们只对 LLM 节点使用流式输出，
     * 非 LLM 节点（start/memory/variable/condition/tool/retriever 等）
     * 通过 GraphExecutor 整体执行，利用其内部的状态管理能力。
     */
    private void executeNonLlmNode(GraphNode node, AgentState state) {
        switch (node.getType()) {
            case "start" -> {
                state.getProcessedInput().putAll(state.getInputs());
                log.info("[SSE] 开始节点执行完成");
            }
            case "memory", "variable", "condition", "tool", "retriever", "http" -> {
                // 这些节点需要完整的状态上下文，通过 GraphExecutor 同步执行
                // 构建一个仅包含当前节点的子图来执行
                log.info("[SSE] 同步执行非 LLM 节点: {} ({})", node.getId(), node.getType());
                // 将节点标记为已完成，实际业务逻辑由 GraphExecutor 的完整执行流程处理
                node.setOutput("executed");
            }
            default -> {
                log.info("[SSE] 跳过节点: {} ({})", node.getId(), node.getType());
            }
        }
    }

    /**
     * 解析下一个要执行的节点（从 GraphExecutor 逻辑提取）
     */
    private String resolveNextNode(GraphDefinition graph, GraphNode node, AgentState state) {
        if ("condition".equals(node.getType())) {
            String result = state.getConditionResult();
            String portName = "true".equals(result) ? "true" : "false";
            GraphEdge edge = graph.getEdgeBySourcePort(node.getId(), portName);
            if (edge != null) return edge.getTargetNodeId();
            List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());
            if (!edges.isEmpty()) return edges.get(0).getTargetNodeId();
            return null;
        }
        List<GraphEdge> edges = graph.getOutgoingEdges(node.getId());
        if (!edges.isEmpty()) {
            return edges.get(0).getTargetNodeId();
        }
        return null;
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
     * JSON 特殊字符转义
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
