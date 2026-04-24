package com.aiagent.engine.graph;

import com.aiagent.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP 调用执行器
 *
 * 从 GraphExecutor 中提取的 HTTP 节点执行逻辑，
 * 负责构建和发送 HTTP 请求，包含 SSRF 防护和敏感头过滤。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpExecutor {

    private final SsrfValidator ssrfValidator;

    /**
     * 执行 HTTP 节点请求
     *
     * @param url 请求 URL（已解析变量引用）
     * @param method HTTP 方法
     * @param nodeConfig 节点配置（包含 headers、body 等）
     * @param stateInputs 状态输入（用于 POST/PUT/PATCH 默认请求体）
     * @return HttpResult 包含响应体和状态码的结果对象
     * @throws BusinessException 如果请求失败
     */
    public HttpResult execute(String url, String method, Map<String, Object> nodeConfig, Map<String, Object> stateInputs) {
        log.info("HTTP 节点: {} {}", method, url);

        if (url == null || url.isBlank()) {
            log.error("HTTP 节点缺少 url 配置");
            return new HttpResult("", null, "Missing URL configuration");
        }

        try {
            // SSRF防护：验证URL安全性
            ssrfValidator.validateHttpUrl(url);

            RestTemplate restTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory reqFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
            reqFactory.setConnectTimeout(10_000);
            reqFactory.setReadTimeout(30_000);

            // 构建请求头
            HttpHeaders headers = buildHeaders(nodeConfig);

            // 构建请求体
            Object body = buildBody(method, nodeConfig, stateInputs);

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

            log.info("HTTP 节点发送请求: {} {}", httpMethod, url);
            ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);

            String responseBody = response.getBody();
            log.info("HTTP 节点响应: status={}, bodyLength={}", response.getStatusCode(), responseBody != null ? responseBody.length() : 0);

            return new HttpResult(responseBody != null ? responseBody : "", response.getStatusCode().value(), null);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTTP 节点调用失败: {} {} - {}", method, url, e.getMessage());
            return new HttpResult("", null, e.getMessage());
        }
    }

    /**
     * 构建请求头，过滤敏感头防止 SSRF 攻击
     */
    private HttpHeaders buildHeaders(Map<String, Object> nodeConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (nodeConfig.containsKey("headers")) {
            // 节点配置中的 headers 是 Map<String, Object> 中的 Object，需要强制转换
            @SuppressWarnings("unchecked")
            for (Map.Entry<String, String> header : headerMap.entrySet()) {
                // 过滤敏感头，防止SSRF攻击
                if ("host".equalsIgnoreCase(header.getKey()) ||
                    "authorization".equalsIgnoreCase(header.getKey()) ||
                    "cookie".equalsIgnoreCase(header.getKey()) ||
                    "set-cookie".equalsIgnoreCase(header.getKey())) {
                    continue;
                }
                headers.set(header.getKey(), header.getValue());
            }
        }
        return headers;
    }

    /**
     * 构建请求体
     */
    private Object buildBody(String method, Map<String, Object> nodeConfig, Map<String, Object> stateInputs) {
        if (nodeConfig.containsKey("body")) {
            return nodeConfig.get("body");
        } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return stateInputs;
        }
        return null;
    }

    /**
     * HTTP 请求结果
     */
    public static class HttpResult {
        private final String body;
        private final Integer statusCode;
        private final String error;

        public HttpResult(String body, Integer statusCode, String error) {
            this.body = body;
            this.statusCode = statusCode;
            this.error = error;
        }

        public String getBody() {
            return body;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public String getError() {
            return error;
        }

        public boolean hasError() {
            return error != null;
        }
    }
}
