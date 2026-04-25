package com.aiagent.config;

import com.aiagent.entity.ApiCallAuditLog;
import com.aiagent.service.ApiCallAuditLogService;
import com.aiagent.tenant.TenantContextHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 请求/响应日志过滤器
 *
 * 记录每个 API 请求的关键信息:
 * - HTTP 方法、请求路径、查询参数
 * - 请求体（POST/PUT，带敏感字段遮蔽）
 * - 响应体（截断到 1000 字符，带敏感字段遮蔽）
 * - 响应状态码、处理耗时
 * - 客户端信息（IP, User-Agent, Accept-Language）
 *
 * 安全特性:
 * - 不记录 Authorization 头部
 * - 对敏感路径（/auth/**）仅记录基本信息
 * - 敏感查询参数（password, token, secret, key）值会被遮蔽
 * - 请求体/响应体中的敏感字段会被遮蔽
 *
 * 日志关联:
 * - 使用 MDC 中的 traceId 和 tenantId
 * - 通过 X-Request-ID 响应头返回请求 ID
 *
 * 审计持久化:
 * - 异步将日志保存到 api_call_audit_logs 表
 *
 * 可配置日志级别:
 * - 通过 ENDPOINT_LOG_LEVELS 配置不同端点的日志级别
 */
@Component
public class RequestResponseLoggingFilter implements Filter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    /** 需要遮蔽值的查询参数名 */
    private static final Set<String> SENSITIVE_PARAMS = new HashSet<>(Arrays.asList(
            "password", "token", "secret", "key", "apikey", "api_key",
            "access_token", "refresh_token", "credential"
    ));

    /** 请求体/响应体中需要遮蔽的 JSON 字段名 */
    private static final Set<String> SENSITIVE_BODY_FIELDS = new HashSet<>(Arrays.asList(
            "password", "token", "accessToken", "refreshToken", "secret", "apiKey",
            "api_key", "accessKey", "secretKey", "credential", "authorization",
            "cardNumber", "cvv", "idCard", "idCardNumber"
    ));

    /** 不记录详细信息的路径前缀（仅记录基本日志） */
    private static final Set<String> SENSITIVE_PATH_PREFIXES = new HashSet<>(Arrays.asList(
            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh"
    ));

    /** 不记录的路径前缀（健康检查等） */
    private static final Set<String> SKIP_PATH_PREFIXES = new HashSet<>(Arrays.asList(
            "/api/actuator"
    ));

    /** 以 DEBUG 级别记录日志的路径前缀 */
    private static final Set<String> DEBUG_LOG_PATH_PREFIXES = new HashSet<>(Arrays.asList(
            "/api/dashboard", "/v1/cache-stats", "/v1/rate-limits", "/actuator"
    ));

    /** 响应体最大记录长度 */
    private static final int MAX_RESPONSE_BODY_LOG_LENGTH = 1000;

    /** 请求体最大记录长度 */
    private static final int MAX_REQUEST_BODY_LOG_LENGTH = 2000;

    /** 是否启用审计日志持久化 */
    private static final boolean AUDIT_LOG_ENABLED = true;

    private final ApiCallAuditLogService auditLogService;

    public RequestResponseLoggingFilter(ApiCallAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String queryString = sanitizeQueryString(httpRequest.getQueryString());
        String clientIp = getClientIp(httpRequest);
        String traceId = MDC.get("traceId");
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 跳过健康检查等路径的日志
        if (shouldSkipPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 收集客户端信息
        String userAgent = truncateHeader(httpRequest.getHeader("User-Agent"), 500);
        String acceptLanguage = truncateHeader(httpRequest.getHeader("Accept-Language"), 100);

        // 包装请求以支持多次读取请求体
        boolean shouldReadBody = shouldReadRequestBody(method, path);
        ContentCachingRequestWrapper wrappedRequest = shouldReadBody
                ? new ContentCachingRequestWrapper(httpRequest) : null;
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        // 设置请求 ID 到响应头
        httpResponse.setHeader("X-Request-ID", requestId);

        long startTime = System.currentTimeMillis();

        try {
            if (wrappedRequest != null) {
                chain.doFilter(wrappedRequest, wrappedResponse);
            } else {
                chain.doFilter(request, wrappedResponse);
            }
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = wrappedResponse.getStatus();

            // 获取请求体和响应体
            String requestBody = null;
            if (wrappedRequest != null) {
                requestBody = getRequestBody(wrappedRequest);
            }
            String responseBody = getResponseBody(wrappedResponse);

            // 构建日志消息
            String logMessage = buildLogMessage(method, path, queryString, clientIp,
                    status, duration, userAgent, requestBody, responseBody);

            // 确定日志级别
            String effectiveLogLevel = resolveLogLevel(path, status, duration);

            // 根据日志级别输出
            switch (effectiveLogLevel) {
                case "ERROR" -> log.error("[API] {}", logMessage);
                case "WARN" -> log.warn("[API] {}", logMessage);
                case "DEBUG" -> log.debug("[API] {}", logMessage);
                default -> log.info("[API] {}", logMessage);
            }

            // 异步持久化审计日志
            if (AUDIT_LOG_ENABLED) {
                persistAuditLog(requestId, traceId, clientIp, userAgent, acceptLanguage,
                        method, path, queryString, requestBody, status, responseBody,
                        duration, effectiveLogLevel);
            }

            // 将响应体写回原始响应
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * 判断是否需要读取请求体
     */
    private boolean shouldReadRequestBody(String method, String path) {
        // 仅对 POST/PUT/PATCH 读取请求体
        if (!("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method))) {
            return false;
        }
        // 敏感路径不读取请求体
        for (String prefix : SENSITIVE_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取请求体内容（带敏感字段遮蔽）
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getCachedContent();
        if (buf == null || buf.length == 0) return null;
        String body = new String(buf, StandardCharsets.UTF_8);
        return maskSensitiveFields(body);
    }

    /**
     * 获取响应体内容（截断 + 敏感字段遮蔽）
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf == null || buf.length == 0) return null;
        String body = new String(buf, StandardCharsets.UTF_8);
        body = maskSensitiveFields(body);
        if (body.length() > MAX_RESPONSE_BODY_LOG_LENGTH) {
            body = body.substring(0, MAX_RESPONSE_BODY_LOG_LENGTH) + "...[truncated]";
        }
        return body;
    }

    /**
     * 遮蔽 JSON 中的敏感字段值
     */
    private String maskSensitiveFields(String content) {
        if (content == null || content.isEmpty()) return content;
        String masked = content;
        for (String field : SENSITIVE_BODY_FIELDS) {
            // 匹配 "fieldName": "value" 或 "fieldName":"value" 模式
            masked = masked.replaceAll(
                    "(?i)(\"" + field + "\"\\s*:\\s*\")([^\"]*?)(\")",
                    "$1****$3");
            // 匹配 "fieldName": value (非字符串值)
            masked = masked.replaceAll(
                    "(?i)(\"" + field + "\"\\s*:\\s*)([^\",{}\\]\\s]+)",
                    "$1****");
        }
        return masked;
    }

    /**
     * 确定日志级别
     */
    private String resolveLogLevel(String path, int status, long duration) {
        // 状态码优先
        if (status >= 500) return "ERROR";
        if (status >= 400) return "WARN";
        if (status == 429) return "WARN";
        if (duration > 3000) return "WARN";

        // 特定路径使用 DEBUG 级别
        for (String prefix : DEBUG_LOG_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return "DEBUG";
            }
        }

        return "INFO";
    }

    /**
     * 构建日志消息
     */
    private String buildLogMessage(String method, String path, String queryString,
                                    String clientIp, int status, long duration,
                                    String userAgent, String requestBody, String responseBody) {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(path);
        if (queryString != null && !queryString.isEmpty()) {
            sb.append("?").append(queryString);
        }
        sb.append(" -> ").append(status);
        sb.append(" (").append(duration).append("ms)");
        sb.append(" [ip=").append(clientIp).append("]");

        // 客户端信息
        if (userAgent != null && !userAgent.isEmpty()) {
            sb.append(" [ua=").append(truncate(userAgent, 80)).append("]");
        }

        // 请求体（仅非敏感路径）
        if (requestBody != null && !requestBody.isEmpty()) {
            String truncatedBody = requestBody.length() > MAX_REQUEST_BODY_LOG_LENGTH
                    ? requestBody.substring(0, MAX_REQUEST_BODY_LOG_LENGTH) + "...[truncated]"
                    : requestBody;
            sb.append(" [body=").append(truncatedBody).append("]");
        }

        // 响应体（截断）
        if (responseBody != null && !responseBody.isEmpty()) {
            sb.append(" [resp=").append(responseBody).append("]");
        }

        return sb.toString();
    }

    /**
     * 遮蔽敏感查询参数的值
     */
    private String sanitizeQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return queryString;
        }

        String[] params = queryString.split("&");
        StringBuilder sanitized = new StringBuilder();

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sanitized.append("&");
            }

            String param = params[i];
            int eqIdx = param.indexOf('=');
            if (eqIdx > 0) {
                String paramName = param.substring(0, eqIdx).toLowerCase();
                if (SENSITIVE_PARAMS.contains(paramName)) {
                    sanitized.append(paramName).append("=****");
                } else {
                    sanitized.append(param);
                }
            } else {
                sanitized.append(param);
            }
        }

        return sanitized.toString();
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 判断是否跳过该路径的日志记录
     */
    private boolean shouldSkipPath(String path) {
        if (path == null) return false;
        return SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * 异步持久化审计日志
     */
    private void persistAuditLog(String requestId, String traceId, String clientIp,
                                  String userAgent, String acceptLanguage,
                                  String method, String path, String queryString,
                                  String requestBody, int status, String responseBody,
                                  long duration, String logLevel) {
        try {
            ApiCallAuditLog auditLog = new ApiCallAuditLog();
            auditLog.setRequestId(requestId);
            auditLog.setTraceId(traceId);
            auditLog.setClientIp(clientIp);
            auditLog.setUserAgent(userAgent);
            auditLog.setAcceptLanguage(acceptLanguage);
            auditLog.setRequestMethod(method);
            auditLog.setRequestPath(path);
            auditLog.setQueryParams(queryString);
            auditLog.setRequestBody(requestBody);
            auditLog.setResponseStatus(status);
            auditLog.setResponseBody(responseBody);
            auditLog.setExecutionTime((int) duration);
            auditLog.setLogLevel(logLevel);

            // 设置租户和用户信息
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                auditLog.setTenantId(tenantId);
            }

            auditLogService.saveAuditLog(auditLog);
        } catch (Exception e) {
            log.debug("持久化审计日志失败: {}", e.getMessage());
        }
    }

    private String truncateHeader(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    @Override
    public int getOrder() {
        // 在 TenantContextFilter 之后执行，确保 MDC 中已有 tenantId
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    // ==================== 内部包装类 ====================

    /**
     * 请求体缓存包装器，支持多次读取请求体
     */
    private static class ContentCachingRequestWrapper extends HttpServletRequestWrapper {

        private final ByteArrayOutputStream cachedContent = new ByteArrayOutputStream();
        private boolean cached = false;

        public ContentCachingRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(super.getInputStream(), cachedContent);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
        }

        public byte[] getCachedContent() {
            if (!cached) {
                cached = true;
            }
            return cachedContent.toByteArray();
        }
    }

    /**
     * 缓存请求体内容的 ServletInputStream
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final InputStream source;
        private final ByteArrayOutputStream copy;

        public CachedBodyServletInputStream(InputStream source, ByteArrayOutputStream copy) {
            this.source = source;
            this.copy = copy;
        }

        @Override
        public int read() throws IOException {
            int b = source.read();
            if (b != -1) {
                copy.write(b);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = source.read(b, off, len);
            if (n > 0) {
                copy.write(b, off, n);
            }
            return n;
        }

        @Override
        public boolean isFinished() {
            try {
                return source.available() == 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // 不支持异步读取
        }
    }

    /**
     * 响应体缓存包装器，支持读取响应体内容
     */
    private static class ContentCachingResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream cachedContent = new ByteArrayOutputStream();
        private ServletOutputStream outputStream;
        private PrintWriter writer;

        public ContentCachingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (outputStream == null) {
                outputStream = new CachedBodyServletOutputStream(
                        super.getOutputStream(), cachedContent);
            }
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(
                        getOutputStream(), getCharacterEncoding()), true);
            }
            return writer;
        }

        public byte[] getContentAsByteArray() {
            return cachedContent.toByteArray();
        }

        public void copyBodyToResponse() throws IOException {
            if (cachedContent.size() > 0) {
                getResponse().getOutputStream().write(cachedContent.toByteArray());
                cachedContent.reset();
            }
        }
    }

    /**
     * 缓存响应体内容的 ServletOutputStream
     */
    private static class CachedBodyServletOutputStream extends ServletOutputStream {

        private final OutputStream target;
        private final ByteArrayOutputStream copy;

        public CachedBodyServletOutputStream(OutputStream target, ByteArrayOutputStream copy) {
            this.target = target;
            this.copy = copy;
        }

        @Override
        public void write(int b) throws IOException {
            target.write(b);
            copy.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            target.write(b, off, len);
            copy.write(b, off, len);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // 不支持异步写入
        }
    }
}
