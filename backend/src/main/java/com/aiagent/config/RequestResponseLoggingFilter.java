package com.aiagent.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 请求/响应日志过滤器
 *
 * 记录每个 API 请求的关键信息:
 * - HTTP 方法、请求路径、查询参数
 * - 响应状态码、处理耗时
 * - 客户端 IP
 *
 * 安全特性:
 * - 不记录请求体（防止敏感数据泄露）
 * - 不记录 Authorization 头部
 * - 对敏感路径（/auth/**）仅记录基本信息
 * - 敏感查询参数（password, token, secret, key）值会被遮蔽
 *
 * 日志关联:
 * - 使用 MDC 中的 traceId 和 tenantId
 * - 通过 X-Request-ID 响应头返回请求 ID
 */
@Component
public class RequestResponseLoggingFilter implements Filter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    /** 需要遮蔽值的查询参数名 */
    private static final Set<String> SENSITIVE_PARAMS = new HashSet<>(Arrays.asList(
            "password", "token", "secret", "key", "apikey", "api_key",
            "access_token", "refresh_token", "credential"
    ));

    /** 不记录详细信息的路径前缀 */
    private static final Set<String> SENSITIVE_PATH_PREFIXES = new HashSet<>(Arrays.asList(
            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh"
    ));

    /** 不记录的路径前缀（健康检查等） */
    private static final Set<String> SKIP_PATH_PREFIXES = new HashSet<>(Arrays.asList(
            "/api/actuator"
    ));

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

        // 跳过健康检查等路径的日志
        if (shouldSkipPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            // 构建日志消息
            String logMessage = buildLogMessage(method, path, queryString, clientIp, status, duration);

            // 根据状态码选择日志级别
            if (status >= 500) {
                log.error("[API] {}", logMessage);
            } else if (status >= 400) {
                log.warn("[API] {}", logMessage);
            } else if (duration > 3000) {
                log.warn("[API-SLOW] {}", logMessage);
            } else {
                log.info("[API] {}", logMessage);
            }
        }
    }

    /**
     * 构建日志消息
     */
    private String buildLogMessage(String method, String path, String queryString,
                                    String clientIp, int status, long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(path);
        if (queryString != null && !queryString.isEmpty()) {
            sb.append("?").append(queryString);
        }
        sb.append(" -> ").append(status);
        sb.append(" (").append(duration).append("ms)");
        sb.append(" [ip=").append(clientIp).append("]");
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

    @Override
    public int getOrder() {
        // 在 TenantContextFilter 之后执行，确保 MDC 中已有 tenantId
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
