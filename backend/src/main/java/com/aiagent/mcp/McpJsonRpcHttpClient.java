package com.aiagent.mcp;

import com.aiagent.config.properties.AiAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * MCP 与远端通信的 JSON-RPC 传输层（当前：HTTP POST + JSON）。
 * 将连接/读超时与业务编排解耦，便于后续在不改动 {@link McpToolGateway} 主流程的前提下
 * 扩展 Streamable HTTP / SSE 等传输（参见 MCP 规范中的 Transports 章节）。
 */
@Component
public class McpJsonRpcHttpClient {

    private static final Logger log = LoggerFactory.getLogger(McpJsonRpcHttpClient.class);

    private final RestTemplate restTemplate;

    public McpJsonRpcHttpClient(AiAgentProperties aiAgentProperties) {
        this.restTemplate = new RestTemplate();
        AiAgentProperties.Mcp mcp = Objects.requireNonNullElse(
                aiAgentProperties.getMcp(), new AiAgentProperties.Mcp());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(mcp.getConnectTimeoutMs());
        factory.setReadTimeout(mcp.getReadTimeoutMs());
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * 向 MCP Server URL 发送 JSON 请求体，返回原始响应字符串（可能为空，例如部分通知的 HTTP 200 无 body）。
     */
    public String postJson(String serverUrl, String requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json, text/event-stream");
            if (log.isDebugEnabled()) {
                log.debug("[MCP transport] request to {}: {}", serverUrl, requestBody);
            }
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    serverUrl, HttpMethod.POST, entity, String.class);
            String body = response.getBody();
            if (log.isDebugEnabled()) {
                log.debug("[MCP transport] response from {}: {}", serverUrl, body);
            }
            return body;
        } catch (Exception e) {
            if (e instanceof org.springframework.web.client.HttpStatusCodeException httpEx) {
                String errBody = httpEx.getResponseBodyAsString();
                log.warn("[MCP transport] HTTP error from {}: status={}, body={}",
                        serverUrl, httpEx.getStatusCode(), errBody);
            } else {
                log.warn("[MCP transport] request failed for {}: {}", serverUrl, e.getMessage());
            }
            throw e;
        }
    }
}
