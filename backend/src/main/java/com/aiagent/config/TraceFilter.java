package com.aiagent.config;

import io.micrometer.core.instrument.Timer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class TraceFilter implements Filter, Ordered {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String RESPONSE_TIME_HEADER = "X-Response-Time";
    private static final String RESPONSE_TIME_MDC = "responseTimeMs";

    private final Timer httpResponseTimeTimer;

    public TraceFilter(Timer httpResponseTimeTimer) {
        this.httpResponseTimeTimer = httpResponseTimeTimer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TRACE_ID, traceId);

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);

            long elapsed = System.currentTimeMillis() - startTime;
            httpResponse.setHeader(REQUEST_ID_HEADER, traceId);
            httpResponse.setHeader(RESPONSE_TIME_HEADER, elapsed + "ms");
            MDC.put(RESPONSE_TIME_MDC, String.valueOf(elapsed));

            // 记录 Prometheus 直方图指标 (method, uri, status)
            int status = httpResponse.getStatus();
            String method = httpRequest.getMethod();
            String uri = normalizeUri(httpRequest.getRequestURI());
            httpResponseTimeTimer.record(
                    Duration.ofMillis(elapsed),
                    io.micrometer.core.instrument.Tags.of(
                            "method", method,
                            "uri", uri,
                            "status", String.valueOf(status)
                    )
            );

            // 慢请求告警: 超过 3 秒记录 warn 日志
            if (elapsed > 3000) {
                org.slf4j.LoggerFactory.getLogger(TraceFilter.class)
                        .warn("[SlowRequest] {} {} 耗时 {}ms",
                                httpRequest.getMethod(), httpRequest.getRequestURI(), elapsed);
            }
        } finally {
            MDC.clear();
        }
    }

    /**
     * 规范化 URI，将路径参数替换为占位符以减少基数
     * 例如: /api/v1/agents/123 -> /api/v1/agents/{id}
     */
    private String normalizeUri(String uri) {
        // 替换 UUID 模式
        String normalized = uri.replaceAll(
                "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{uuid}");
        // 替换纯数字 ID
        normalized = normalized.replaceAll("/\\d+(?=/|$)", "/{id}");
        return normalized;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
