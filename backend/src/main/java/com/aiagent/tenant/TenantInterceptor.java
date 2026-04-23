package com.aiagent.tenant;

import com.aiagent.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    @Value("${ai-agent.tenant.default-schema:public}")
    private String defaultSchema;

    @Value("${ai-agent.tenant.schema-prefix:t_}")
    private String schemaPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantIdStr = request.getHeader("X-Tenant-ID");
        Long tenantId = null;

        if (tenantIdStr != null && !tenantIdStr.isEmpty()) {
            try {
                tenantId = Long.parseLong(tenantIdStr);
            } catch (NumberFormatException e) {
                log.warn("无效的租户ID格式: {}", tenantIdStr);
                TenantContextHolder.setSchemaName(defaultSchema);
                return true;
            }
        }

        // If user is authenticated, validate that the header tenant matches the JWT tenant
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            Long jwtTenantId = userPrincipal.getTenantId();
            if (jwtTenantId != null) {
                // Authenticated user must use their own tenant
                if (tenantId != null && !tenantId.equals(jwtTenantId)) {
                    log.warn("租户越权访问: 用户JWT租户ID={}, 请求头租户ID={}", jwtTenantId, tenantId);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"message\":\"租户越权访问\"}");
                    return false;
                }
                // Use the tenant from JWT (trust the token, not the header)
                tenantId = jwtTenantId;
            }
        }

        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setSchemaName(schemaPrefix + tenantId);
        } else {
            TenantContextHolder.setSchemaName(defaultSchema);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
}
