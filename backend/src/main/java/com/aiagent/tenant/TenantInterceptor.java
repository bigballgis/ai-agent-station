package com.aiagent.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantIdStr = request.getHeader("X-Tenant-ID");
        if (tenantIdStr != null && !tenantIdStr.isEmpty()) {
            try {
                Long tenantId = Long.parseLong(tenantIdStr);
                TenantContextHolder.setTenantId(tenantId);
                TenantContextHolder.setSchemaName(schemaPrefix + tenantId);
            } catch (NumberFormatException e) {
                log.warn("无效的租户ID格式: {}", tenantIdStr);
                TenantContextHolder.setSchemaName(defaultSchema);
            }
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
