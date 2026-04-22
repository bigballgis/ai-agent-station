package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.service.llm.LangChain4jService;
import com.aiagent.security.PromptInjectionFilter;
import com.aiagent.security.PromptInjectionFilter.FilterResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
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
    private final Executor sseExecutor;

    public StreamController(LangChain4jService langChain4jService,
                            PromptInjectionFilter promptInjectionFilter,
                            @Qualifier("sseExecutor") Executor sseExecutor) {
        this.langChain4jService = langChain4jService;
        this.promptInjectionFilter = promptInjectionFilter;
        this.sseExecutor = sseExecutor;
    }

    /**
     * SSE 流式对话（GET）
     * 
     * GET /api/v1/stream/chat?provider=openai&message=你好&sessionId=xxx
     */
    @RequiresPermission("agent:invoke")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
     * 流式返回 Agent 图执行的每个节点状态:
     * - event: node_start / data: {"nodeId":"llm-1","nodeType":"llm"}
     * - event: token     / data: {"content":"..."}
     * - event: node_end   / data: {"nodeId":"llm-1","status":"completed"}
     * - event: done       / data: {"content":"最终输出"}
     */
    @Operation(summary = "SSE流式对话(POST)")
    @GetMapping(value = "/agent/{agentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgentExecution(
            @PathVariable Long agentId,
            @RequestParam String message,
            @RequestParam(required = false) String sessionId) {

        log.info("[SSE] Agent 流式执行: agentId={}", agentId);

        SseEmitter emitter = new SseEmitter(300_000L);

        sseExecutor.execute(() -> {
            try {
                // TODO: 接入 GraphExecutor 逐步执行回调
                // 当前使用 langchain4j 流式输出作为模拟
                emitter.send(SseEmitter.event()
                        .name("node_start")
                        .data("{\"nodeId\":\"start-1\",\"nodeType\":\"start\"}"));

                Thread.sleep(200);

                emitter.send(SseEmitter.event()
                        .name("node_start")
                        .data("{\"nodeId\":\"llm-1\",\"nodeType\":\"llm\"}"));

                // 流式输出 LLM 响应
                langChain4jService.chatStream("openai", "你是一个 AI 助手。", message,
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
                                            .data("{\"content\":\"" + escapeJson(token) + "\"}"));
                                } catch (IOException e) {
                                    subscription.cancel();
                                }
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data("{\"content\":\"" + escapeJson(throwable.getMessage()) + "\"}"));
                                } catch (IOException ignored) {}
                            }

                            @Override
                            public void onComplete() {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("node_end")
                                            .data("{\"nodeId\":\"llm-1\",\"status\":\"completed\"}"));
                                    emitter.send(SseEmitter.event()
                                            .name("node_start")
                                            .data("{\"nodeId\":\"end-1\",\"nodeType\":\"end\"}"));
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data("{\"type\":\"done\",\"content\":\"\"}"));
                                    emitter.complete();
                                } catch (IOException ignored) {}
                            }
                        });
            } catch (Exception e) {
                log.error("[SSE] Agent 执行失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"content\":\"" + escapeJson(e.getMessage()) + "\"}"));
                    emitter.complete();
                } catch (IOException ignored) {}
            }
        });

        return emitter;
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
