package com.aiagent.mcp;

import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.entity.McpTool;
import com.aiagent.entity.McpToolCallLog;
import com.aiagent.entity.McpToolCallLog.ErrorCategory;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.exception.ServiceUnavailableException;
import com.aiagent.exception.ValidationException;
import com.aiagent.repository.McpToolCallLogRepository;
import com.aiagent.repository.McpToolRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Tool Gateway — JSON-RPC 2.0 协议与 MCP 方法（initialize、tools/*、通知）协调。
 * <p>
 * 协议随官方规范按<strong>日期版本</strong>演进（如 2025-11-25），与 JSON-RPC 的「2.0」不是同一件事；
 * 具体协商版本由 {@link AiAgentProperties.Mcp#protocolVersion} 配置。传输层见 {@link McpJsonRpcHttpClient}。
 * 
 * JSON-RPC 2.0 请求格式:
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "tools/call",
 *   "params": { "name": "tool_name", "arguments": {...} }
 * }
 * 
 * JSON-RPC 2.0 响应格式:
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "result": { "content": [{ "type": "text", "text": "..." }] }
 * }
 */
@Service
public class McpToolGateway {

    private static final Logger log = LoggerFactory.getLogger(McpToolGateway.class);

    private final McpToolRepository mcpToolRepository;
    private final McpToolCallLogRepository mcpToolCallLogRepository;
    private final ObjectMapper objectMapper;
    private final AiAgentProperties aiAgentProperties;
    private final McpJsonRpcHttpClient mcpJsonRpcClient;
    /** 非 MCP 类型的 HTTP 工具调用（与 JSON-RPC 传输解耦，共享超时配置） */
    private final RestTemplate httpToolRestTemplate;

    /**
     * MCP 服务器会话缓存: serverUrl -> McpSession
     */
    private final Map<String, McpSession> sessionCache = new ConcurrentHashMap<>();

    /**
     * 工具列表缓存
     */
    private volatile List<Map<String, Object>> toolsCache;
    private volatile long toolsCacheTime = 0;
    private static final long TOOLS_CACHE_TTL_MS = 30_000;

    public McpToolGateway(McpToolRepository mcpToolRepository,
                          McpToolCallLogRepository mcpToolCallLogRepository,
                          ObjectMapper objectMapper,
                          AiAgentProperties aiAgentProperties,
                          McpJsonRpcHttpClient mcpJsonRpcClient) {
        this.mcpToolRepository = mcpToolRepository;
        this.mcpToolCallLogRepository = mcpToolCallLogRepository;
        this.objectMapper = objectMapper;
        this.aiAgentProperties = aiAgentProperties;
        this.mcpJsonRpcClient = mcpJsonRpcClient;
        AiAgentProperties.Mcp m = aiAgentProperties.getMcp() != null
                ? aiAgentProperties.getMcp() : new AiAgentProperties.Mcp();
        this.httpToolRestTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory httpFactory = new SimpleClientHttpRequestFactory();
        httpFactory.setConnectTimeout(m.getConnectTimeoutMs());
        httpFactory.setReadTimeout(m.getReadTimeoutMs());
        this.httpToolRestTemplate.setRequestFactory(httpFactory);
    }

    private AiAgentProperties.Mcp mcpConfig() {
        return aiAgentProperties.getMcp() != null ? aiAgentProperties.getMcp() : new AiAgentProperties.Mcp();
    }

    // ==================== 工具发现 ====================

    /**
     * 获取所有可用工具列表（兼容 langchain4j ToolProvider）
     */
    public List<Map<String, Object>> getAvailableTools() {
        long now = System.currentTimeMillis();
        if (toolsCache != null && (now - toolsCacheTime) < TOOLS_CACHE_TTL_MS) {
            return toolsCache;
        }

        List<McpTool> tools = mcpToolRepository.findByIsActiveTrue();

        List<Map<String, Object>> result = new ArrayList<>();
        for (McpTool tool : tools) {
            Map<String, Object> toolMap = new LinkedHashMap<>();
            toolMap.put("id", tool.getId());
            toolMap.put("name", tool.getToolName());
            toolMap.put("code", tool.getToolCode());
            toolMap.put("description", tool.getDescription());
            toolMap.put("type", tool.getToolType());

            // 解析 inputSchema
            if (tool.getConfig() != null) {
                try {
                    JsonNode configNode = objectMapper.readTree(tool.getConfig());
                    if (configNode.has("inputSchema")) {
                        toolMap.put("inputSchema", objectMapper.readTree(
                                configNode.get("inputSchema").toString()));
                    }
                } catch (Exception e) {
                    log.warn("解析工具配置失败: toolId={}", tool.getId());
                }
            }

            result.add(toolMap);
        }

        toolsCache = result;
        toolsCacheTime = now;
        return result;
    }

    /**
     * 强制刷新工具缓存
     */
    public void refreshToolsCache() {
        toolsCache = null;
        toolsCacheTime = 0;
        log.info("[MCP Gateway] 工具缓存已刷新");
    }

    // ==================== 工具调用（原有接口，向后兼容） ====================

    /**
     * 调用工具（向后兼容接口）
     * 含 Spring Retry: 最多重试2次，指数退避 (1s, 2s)，仅重试网络和临时错误
     */
    @Retryable(
        value = {ResourceAccessException.class, HttpServerErrorException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "invokeToolRecover"
    )
    public Object invokeTool(Long toolId, Map<String, Object> parameters, Long tenantId, Long apiCallLogId) {
        long startTime = System.currentTimeMillis();
        McpToolCallLog callLog = new McpToolCallLog();
        callLog.setMcpToolId(toolId);
        callLog.setTenantId(tenantId);
        callLog.setApiCallLogId(apiCallLogId);

        try {
            McpTool tool = mcpToolRepository.findById(toolId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tool not found: " + toolId));

            if (!tool.getIsActive()) {
                throw new ValidationException("Tool is not active: " + toolId);
            }

            callLog.setRequestParams(objectMapper.writeValueAsString(parameters));

            Object result;
            if ("MCP".equalsIgnoreCase(tool.getToolType())) {
                // MCP 协议调用（JSON-RPC 2.0）
                result = executeMcpToolCall(tool, parameters);
            } else if ("HTTP".equalsIgnoreCase(tool.getToolType())) {
                result = executeHttpTool(tool, parameters);
            } else if ("CUSTOM".equalsIgnoreCase(tool.getToolType())) {
                result = executeCustomTool(tool, parameters);
            } else {
                throw new ValidationException("Unsupported tool type: " + tool.getToolType());
            }

            callLog.setResponseResult(objectMapper.writeValueAsString(result));
            callLog.setStatus(com.aiagent.entity.ApiCallLog.ApiCallStatus.SUCCESS);
            return result;

        } catch (BusinessException e) {
            log.error("[MCP Gateway] 工具调用业务异常: toolId={}, error={}", toolId, e.getMessage());
            callLog.setErrorMessage(e.getMessage());
            callLog.setErrorCategory(classifyError(e));
            callLog.setStatus(com.aiagent.entity.ApiCallLog.ApiCallStatus.FAILED);
            throw e;
        } catch (ResourceAccessException e) {
            log.error("[MCP Gateway] 工具调用网络异常: toolId={}, error={}", toolId, e.getMessage());
            callLog.setErrorMessage(e.getMessage());
            callLog.setErrorCategory(ErrorCategory.TIMEOUT);
            callLog.setStatus(com.aiagent.entity.ApiCallLog.ApiCallStatus.FAILED);
            throw new ServiceUnavailableException("MCP-Tool-" + toolId, "网络连接失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[MCP Gateway] 工具调用未知异常: toolId={}", toolId, e);
            callLog.setErrorMessage(e.getMessage());
            callLog.setErrorCategory(classifyError(e));
            callLog.setStatus(com.aiagent.entity.ApiCallLog.ApiCallStatus.FAILED);
            throw new BusinessException("error.tool.invocation_failed",
                    "MCP tool invocation failed: " + e.getMessage(), e);
        } finally {
            callLog.setExecutionTime((int) (System.currentTimeMillis() - startTime));
            mcpToolCallLogRepository.save(callLog);
        }
    }

    /**
     * invokeTool() 方法的恢复方法 - 重试耗尽后抛出业务异常
     */
    private Object invokeToolRecover(Exception e, Long toolId, Map<String, Object> parameters,
                                      Long tenantId, Long apiCallLogId) {
        log.error("[MCP Gateway] invokeTool() 重试耗尽, toolId={}, error={}", toolId, e.getMessage());
        throw new ServiceUnavailableException("MCP-Tool-" + toolId,
                "MCP tool invocation failed after retries: " + e.getMessage(), e);
    }

    // ==================== JSON-RPC 2.0 MCP 协议调用 ====================

    /**
     * 通过 MCP 协议调用工具（JSON-RPC 2.0）
     */
    private Object executeMcpToolCall(McpTool tool, Map<String, Object> parameters) {
        String serverUrl = resolveMcpServerUrl(tool);
        if (serverUrl == null) {
            throw new ValidationException(
                    "MCP server URL not configured for tool: " + tool.getToolName());
        }

        // 确保 MCP 服务器已初始化
        McpSession session = getOrCreateSession(serverUrl, tool);

        // 构建 JSON-RPC 2.0 tools/call 请求
        ObjectNode rpcRequest = objectMapper.createObjectNode();
        rpcRequest.put("jsonrpc", McpJsonRpcConstants.JSON_RPC_2_0);
        rpcRequest.put("id", session.nextRequestId());
        rpcRequest.put("method", McpJsonRpcConstants.METHOD_TOOLS_CALL);

        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", tool.getToolCode());
        params.set("arguments", objectMapper.valueToTree(parameters));
        rpcRequest.set("params", params);

        log.info("[MCP Gateway] JSON-RPC 2.0 请求: method=tools/call, tool={}, server={}",
                tool.getToolCode(), serverUrl);

        // 发送 JSON-RPC 2.0 请求
        return sendJsonRpcRequest(serverUrl, rpcRequest);
    }

    /**
     * MCP 协议: 初始化服务器连接
     */
    public Map<String, Object> initializeServer(String serverUrl, Map<String, Object> clientInfo) {
        AiAgentProperties.Mcp mcp = mcpConfig();
        ObjectNode rpcRequest = objectMapper.createObjectNode();
        rpcRequest.put("jsonrpc", McpJsonRpcConstants.JSON_RPC_2_0);
        rpcRequest.put("id", 1);
        rpcRequest.put("method", McpJsonRpcConstants.METHOD_INITIALIZE);

        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", mcp.getProtocolVersion());
        Map<String, String> defaultClient = Map.of(
                "name", mcp.getClientName(),
                "version", mcp.getClientVersion());
        params.set("clientInfo", objectMapper.valueToTree(
                clientInfo != null ? clientInfo : defaultClient));
        params.set("capabilities", objectMapper.createObjectNode());
        rpcRequest.set("params", params);

        Object response = sendJsonRpcRequest(serverUrl, rpcRequest);

        // 创建会话
        McpSession session = new McpSession(serverUrl);
        session.setInitialized(true);
        sessionCache.put(serverUrl, session);

        sendNotificationInitializedIfConfigured(serverUrl, mcp);

        log.info("[MCP Gateway] MCP 服务器初始化成功: server={}, protocolVersion={}", serverUrl, mcp.getProtocolVersion());
        return objectMapper.convertValue(response, Map.class);
    }

    private void sendNotificationInitializedIfConfigured(String serverUrl, AiAgentProperties.Mcp mcp) {
        if (!mcp.isSendInitializedNotification()) {
            return;
        }
        try {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("jsonrpc", McpJsonRpcConstants.JSON_RPC_2_0);
            n.put("method", McpJsonRpcConstants.METHOD_NOTIFICATION_INITIALIZED);
            n.set("params", objectMapper.createObjectNode());
            mcpJsonRpcClient.postJson(serverUrl, objectMapper.writeValueAsString(n));
        } catch (Exception e) {
            log.warn("[MCP Gateway] notifications/initialized 未送达（部分服务端不强制）: {}", e.getMessage());
        }
    }

    /**
     * MCP 协议: 列出服务器提供的工具
     */
    public List<Map<String, Object>> listServerTools(String serverUrl) {
        McpSession session = sessionCache.get(serverUrl);
        int requestId = session != null ? session.nextRequestId() : 1;

        ObjectNode rpcRequest = objectMapper.createObjectNode();
        rpcRequest.put("jsonrpc", McpJsonRpcConstants.JSON_RPC_2_0);
        rpcRequest.put("id", requestId);
        rpcRequest.put("method", McpJsonRpcConstants.METHOD_TOOLS_LIST);
        rpcRequest.set("params", objectMapper.createObjectNode());

        Object response = sendJsonRpcRequest(serverUrl, rpcRequest);

        try {
            JsonNode responseNode = objectMapper.readTree(objectMapper.writeValueAsString(response));
            JsonNode tools = responseNode.path("result").path("tools");
            List<Map<String, Object>> result = new ArrayList<>();
            if (tools.isArray()) {
                for (JsonNode tool : tools) {
                    result.add(objectMapper.convertValue(tool, Map.class));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[MCP Gateway] 解析 tools/list 响应失败: {}", e.getMessage());
            return List.of();
        }
    }

    // ==================== JSON-RPC 2.0 传输层 ====================

    /**
     * 发送 JSON-RPC 2.0 请求并解析响应
     */
    private Object sendJsonRpcRequest(String serverUrl, ObjectNode request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            log.debug("[MCP Gateway] JSON-RPC 请求: {}", requestBody);

            String responseBody = mcpJsonRpcClient.postJson(serverUrl, requestBody);
            log.debug("[MCP Gateway] JSON-RPC 响应: {}", responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                throw new BusinessException("error.tool.empty_response", "MCP server returned empty response");
            }

            // 解析 JSON-RPC 2.0 响应
            JsonNode responseNode = objectMapper.readTree(responseBody);

            // 检查错误
            if (responseNode.has("error")) {
                JsonNode error = responseNode.get("error");
                int code = error.has("code") ? error.get("code").asInt() : -32603;
                String message = error.has("message") ? error.get("message").asText() : "Unknown error";
                throw new BusinessException("error.tool.jsonrpc_error",
                        "JSON-RPC 2.0 Error [" + code + "]: " + message, code, message);
            }

            // 提取 result
            if (responseNode.has("result")) {
                JsonNode result = responseNode.get("result");

                // 处理 MCP content 格式: { "content": [{ "type": "text", "text": "..." }] }
                if (result.has("content") && result.get("content").isArray()) {
                    JsonNode content = result.get("content");
                    StringBuilder textBuilder = new StringBuilder();
                    for (JsonNode item : content) {
                        if ("text".equals(item.path("type").asText())) {
                            textBuilder.append(item.path("text").asText());
                        }
                    }
                    if (!textBuilder.isEmpty()) {
                        return Map.of("content", textBuilder.toString());
                    }
                }

                return objectMapper.convertValue(result, Map.class);
            }

            return responseBody;
        } catch (JsonProcessingException e) {
            throw new BusinessException("error.tool.serialization_failed",
                    "JSON-RPC 2.0 序列化失败: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("MCP-Server", "连接MCP服务器失败: " + e.getMessage(), e);
        }
    }

    // ==================== 会话管理 ====================

    private McpSession getOrCreateSession(String serverUrl, McpTool tool) {
        return sessionCache.computeIfAbsent(serverUrl, url -> {
            log.info("[MCP Gateway] 创建 MCP 会话: {}", url);
            try {
                initializeServer(url, null);
            } catch (Exception e) {
                log.warn("[MCP Gateway] MCP 服务器初始化失败（将尝试直接调用）: {}", e.getMessage());
            }
            return sessionCache.getOrDefault(url, new McpSession(url));
        });
    }

    private String resolveMcpServerUrl(McpTool tool) {
        if (tool.getEndpointUrl() != null && !tool.getEndpointUrl().isBlank()) {
            return tool.getEndpointUrl();
        }
        // 从 config JSON 中解析 serverUrl
        if (tool.getConfig() != null) {
            try {
                JsonNode configNode = objectMapper.readTree(tool.getConfig());
                if (configNode.has("serverUrl")) {
                    return configNode.get("serverUrl").asText();
                }
            } catch (Exception e) {
                log.debug("Failed to parse MCP server URL from tool config: {}", e.getMessage());
            }
        }
        return null;
    }

    // ==================== 传统工具执行 ====================

    private Object executeHttpTool(McpTool tool, Map<String, Object> parameters) {
        String url = tool.getEndpointUrl();
        log.info("[MCP Gateway] HTTP 工具调用: {}, URL: {}", tool.getToolName(), url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 从 config 读取自定义 headers
        if (tool.getConfig() != null) {
            try {
                JsonNode configNode = objectMapper.readTree(tool.getConfig());
                if (configNode.has("headers")) {
                    JsonNode headersNode = configNode.get("headers");
                    Iterator<Map.Entry<String, JsonNode>> fields = headersNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        headers.set(field.getKey(), field.getValue().asText());
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse HTTP headers from tool config: {}", e.getMessage());
            }
        }

        String httpMethod = "POST";
        if (tool.getConfig() != null) {
            try {
                JsonNode configNode = objectMapper.readTree(tool.getConfig());
                if (configNode.has("method")) {
                    httpMethod = configNode.get("method").asText("POST");
                }
            } catch (Exception e) {
                log.debug("Failed to parse HTTP method from tool config: {}", e.getMessage());
            }
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(parameters, headers);
        ResponseEntity<Map> response = httpToolRestTemplate.exchange(
                url, HttpMethod.valueOf(httpMethod), entity, Map.class);
        return response.getBody();
    }

    private Object executeCustomTool(McpTool tool, Map<String, Object> parameters) {
        log.info("[MCP Gateway] 自定义工具执行: {}", tool.getToolName());

        // 解析工具配置中的自定义逻辑提示
        String customType = "echo";
        String customExpression = null;

        if (tool.getConfig() != null) {
            try {
                JsonNode configNode = objectMapper.readTree(tool.getConfig());
                if (configNode.has("customType")) {
                    customType = configNode.get("customType").asText("echo");
                }
                if (configNode.has("expression")) {
                    customExpression = configNode.get("expression").asText(null);
                }
            } catch (Exception e) {
                log.warn("[MCP Gateway] 解析自定义工具配置失败: {}", e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "processed");
        result.put("toolName", tool.getToolName());
        result.put("toolCode", tool.getToolCode());
        result.put("customType", customType);
        result.put("parameters", parameters);

        switch (customType.toLowerCase()) {
            case "script" -> {
                // 支持 script 类型：执行简单表达式并回显结果
                if (customExpression != null && !customExpression.isBlank()) {
                    // 简单表达式处理：替换参数占位符
                    String processed = customExpression;
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        processed = processed.replace("${" + entry.getKey() + "}",
                                entry.getValue() != null ? entry.getValue().toString() : "");
                    }
                    result.put("expression", customExpression);
                    result.put("processedExpression", processed);
                    result.put("output", processed);
                } else {
                    result.put("output", "No expression provided for script type");
                }
            }
            case "transform" -> {
                // 转换类型：将参数键名转为大写并附加前缀
                Map<String, Object> transformed = new LinkedHashMap<>();
                parameters.forEach((k, v) -> transformed.put("transformed_" + k.toUpperCase(), v));
                result.put("output", transformed);
            }
            default -> {
                // 默认 echo 类型：回显参数
                result.put("output", parameters);
            }
        }

        return result;
    }

    // ==================== 错误分类 ====================

    /**
     * 根据异常类型对 MCP 调用失败进行分类
     */
    private ErrorCategory classifyError(Exception e) {
        if (e instanceof ResourceAccessException) {
            // 连接超时或读取超时
            return ErrorCategory.TIMEOUT;
        }
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpEx = (HttpClientErrorException) e;
            if (httpEx.getStatusCode().value() == 401 || httpEx.getStatusCode().value() == 403) {
                return ErrorCategory.AUTH_FAILURE;
            }
            if (httpEx.getStatusCode().value() == 429) {
                return ErrorCategory.RATE_LIMITED;
            }
            if (httpEx.getStatusCode().value() == 400 || httpEx.getStatusCode().value() == 422) {
                return ErrorCategory.INVALID_REQUEST;
            }
            return ErrorCategory.INTERNAL_ERROR;
        }
        if (e instanceof HttpServerErrorException) {
            return ErrorCategory.INTERNAL_ERROR;
        }
        // 根据 errorMessage 关键字做二次判断
        String msg = e.getMessage();
        if (msg != null) {
            String lowerMsg = msg.toLowerCase();
            if (lowerMsg.contains("timeout") || lowerMsg.contains("timed out")) {
                return ErrorCategory.TIMEOUT;
            }
            if (lowerMsg.contains("401") || lowerMsg.contains("403") || lowerMsg.contains("auth") || lowerMsg.contains("unauthorized")) {
                return ErrorCategory.AUTH_FAILURE;
            }
            if (lowerMsg.contains("429") || lowerMsg.contains("rate limit")) {
                return ErrorCategory.RATE_LIMITED;
            }
            if (lowerMsg.contains("400") || lowerMsg.contains("invalid") || lowerMsg.contains("bad request")) {
                return ErrorCategory.INVALID_REQUEST;
            }
        }
        return ErrorCategory.INTERNAL_ERROR;
    }

    // ==================== MCP 会话内部类 ====================

    /**
     * MCP 服务器会话
     */
    private static class McpSession {
        private final String serverUrl;
        private boolean initialized;
        private int lastRequestId;

        McpSession(String serverUrl) {
            this.serverUrl = serverUrl;
            this.lastRequestId = 0;
        }

        int nextRequestId() {
            return ++lastRequestId;
        }

        boolean isInitialized() { return initialized; }
        void setInitialized(boolean initialized) { this.initialized = initialized; }
    }
}
