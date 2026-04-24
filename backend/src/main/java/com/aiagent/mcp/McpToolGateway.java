package com.aiagent.mcp;

import com.aiagent.entity.McpTool;
import com.aiagent.entity.McpToolCallLog;
import com.aiagent.exception.BusinessException;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Tool Gateway — JSON-RPC 2.0 协议合规
 * 
 * 核心能力:
 * 1. JSON-RPC 2.0 协议封装（标准请求/响应格式）
 * 2. MCP 协议方法支持（initialize, tools/list, tools/call）
 * 3. 工具发现与注册（getAvailableTools）
 * 4. 多传输层支持（HTTP POST / SSE）
 * 5. 调用日志与审计
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
    private static final String JSON_RPC_VERSION = "2.0";

    private final McpToolRepository mcpToolRepository;
    private final McpToolCallLogRepository mcpToolCallLogRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

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
                          ObjectMapper objectMapper) {
        this.mcpToolRepository = mcpToolRepository;
        this.mcpToolCallLogRepository = mcpToolCallLogRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        // 设置超时
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) this.restTemplate.getRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
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
     */
    public Object invokeTool(Long toolId, Map<String, Object> parameters, Long tenantId, Long apiCallLogId) {
        long startTime = System.currentTimeMillis();
        McpToolCallLog callLog = new McpToolCallLog();
        callLog.setMcpToolId(toolId);
        callLog.setTenantId(tenantId);
        callLog.setApiCallLogId(apiCallLogId);

        try {
            McpTool tool = mcpToolRepository.findById(toolId)
                    .orElseThrow(() -> new BusinessException("Tool not found: " + toolId));

            if (!tool.getIsActive()) {
                throw new BusinessException("Tool is not active: " + toolId);
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
                throw new BusinessException("Unsupported tool type: " + tool.getToolType());
            }

            callLog.setResponseResult(objectMapper.writeValueAsString(result));
            callLog.setStatus(com.aiagent.entity.ApiCallLog.ApiCallStatus.SUCCESS);
            return result;

        } catch (Exception e) {
            log.error("[MCP Gateway] 工具调用失败: toolId={}", toolId, e);
            callLog.setErrorMessage(e.getMessage());
            callLog.setStatus(com.aiagent.entity.ApiCallLog.ApiCallStatus.FAILED);
            throw new BusinessException("MCP tool invocation failed: " + e.getMessage(), e);
        } finally {
            callLog.setExecutionTime((int) (System.currentTimeMillis() - startTime));
            mcpToolCallLogRepository.save(callLog);
        }
    }

    // ==================== JSON-RPC 2.0 MCP 协议调用 ====================

    /**
     * 通过 MCP 协议调用工具（JSON-RPC 2.0）
     */
    private Object executeMcpToolCall(McpTool tool, Map<String, Object> parameters) {
        String serverUrl = resolveMcpServerUrl(tool);
        if (serverUrl == null) {
            throw new BusinessException("MCP server URL not configured for tool: " + tool.getToolName());
        }

        // 确保 MCP 服务器已初始化
        McpSession session = getOrCreateSession(serverUrl, tool);

        // 构建 JSON-RPC 2.0 tools/call 请求
        ObjectNode rpcRequest = objectMapper.createObjectNode();
        rpcRequest.put("jsonrpc", JSON_RPC_VERSION);
        rpcRequest.put("id", session.nextRequestId());
        rpcRequest.put("method", "tools/call");

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
        ObjectNode rpcRequest = objectMapper.createObjectNode();
        rpcRequest.put("jsonrpc", JSON_RPC_VERSION);
        rpcRequest.put("id", 1);
        rpcRequest.put("method", "initialize");

        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", "2024-11-05");
        params.set("clientInfo", objectMapper.valueToTree(
                clientInfo != null ? clientInfo : Map.of("name", "AI-Agent-Station", "version", "3.0.0")));
        params.set("capabilities", objectMapper.createObjectNode());
        rpcRequest.set("params", params);

        Object response = sendJsonRpcRequest(serverUrl, rpcRequest);

        // 创建会话
        McpSession session = new McpSession(serverUrl);
        session.setInitialized(true);
        sessionCache.put(serverUrl, session);

        log.info("[MCP Gateway] MCP 服务器初始化成功: {}", serverUrl);
        return objectMapper.convertValue(response, Map.class);
    }

    /**
     * MCP 协议: 列出服务器提供的工具
     */
    public List<Map<String, Object>> listServerTools(String serverUrl) {
        McpSession session = sessionCache.get(serverUrl);
        int requestId = session != null ? session.nextRequestId() : 1;

        ObjectNode rpcRequest = objectMapper.createObjectNode();
        rpcRequest.put("jsonrpc", JSON_RPC_VERSION);
        rpcRequest.put("id", requestId);
        rpcRequest.put("method", "tools/list");
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json, text/event-stream");

            String requestBody = objectMapper.writeValueAsString(request);
            log.debug("[MCP Gateway] JSON-RPC 请求: {}", requestBody);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    serverUrl, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            log.debug("[MCP Gateway] JSON-RPC 响应: {}", responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                throw new BusinessException("MCP server returned empty response");
            }

            // 解析 JSON-RPC 2.0 响应
            JsonNode responseNode = objectMapper.readTree(responseBody);

            // 检查错误
            if (responseNode.has("error")) {
                JsonNode error = responseNode.get("error");
                int code = error.has("code") ? error.get("code").asInt() : -32603;
                String message = error.has("message") ? error.get("message").asText() : "Unknown error";
                throw new BusinessException("JSON-RPC 2.0 Error [" + code + "]: " + message);
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
            throw new BusinessException("JSON-RPC 2.0 序列化失败: " + e.getMessage(), e);
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
        ResponseEntity<Map> response = restTemplate.exchange(
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
