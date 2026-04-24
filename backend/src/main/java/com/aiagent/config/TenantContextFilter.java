package com.aiagent.config;

import com.aiagent.security.JwtUtil;
import com.aiagent.tenant.TenantContextHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 租户上下文过滤器
 *
 * 从 JWT Token 中提取 tenantId，并设置到 MDC 中用于日志关联。
 * 同时确保 TenantContextHolder 中的租户信息在请求结束时清理。
 *
 * MDC key: tenantId
 * 日志格式中可通过 %X{tenantId} 引用
 *
 * 注意: 此过滤器在 TraceFilter 之后、JwtAuthenticationFilter 之前执行。
 * JwtAuthenticationFilter 会设置 TenantContextHolder，此过滤器仅负责 MDC 同步。
 */
@Component
public class TenantContextFilter implements Filter, Ordered {

    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";

    private final JwtUtil jwtUtil;

    public TenantContextFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String token = extractJwt(httpRequest);

            if (StringUtils.hasText(token)) {
                try {
                    Long tenantId = jwtUtil.getTenantIdFromToken(token);
                    Long userId = jwtUtil.getUserIdFromToken(token);

                    if (tenantId != null) {
                        MDC.put(MDC_TENANT_ID, String.valueOf(tenantId));
                    }
                    if (userId != null) {
                        MDC.put(MDC_USER_ID, String.valueOf(userId));
                    }
                } catch (Exception e) {
                    // Token 无效或已过期，不设置 MDC 值
                }
            }

            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private String extractJwt(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        // 在 TraceFilter (HIGHEST_PRECEDENCE) 之后执行
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
