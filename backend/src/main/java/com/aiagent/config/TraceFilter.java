package com.aiagent.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceFilter implements Filter, Ordered {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String RESPONSE_TIME_HEADER = "X-Response-Time";
    private static final String RESPONSE_TIME_MDC = "responseTimeMs";

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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
