package com.aiagent.security;

import com.aiagent.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final ApiKeyService apiKeyService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ApiKeyService apiKeyService) {
        this.jwtUtil = jwtUtil;
        this.apiKeyService = apiKeyService;
    }

    @Value("${ai-agent.tenant.default-schema:public}")
    private String defaultSchema;

    @Value("${ai-agent.tenant.schema-prefix:t_}")
    private String schemaPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);
            String apiKey = getApiKeyFromRequest(request);

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                Long tenantId = jwtUtil.getTenantIdFromToken(token);

                setTenantContext(tenantId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(userId, username, tenantId),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (StringUtils.hasText(apiKey) && apiKeyService.validateApiKey(apiKey)) {
                Long tenantId = apiKeyService.getTenantIdByApiKey(apiKey);
                setTenantContext(tenantId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(null, "api-user", tenantId),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_API"))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.warn("无法设置用户认证", e);
        }

        filterChain.doFilter(request, response);
    }

    private void setTenantContext(Long tenantId) {
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setSchemaName(schemaPrefix + tenantId);
        } else {
            TenantContextHolder.setSchemaName(defaultSchema);
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getApiKeyFromRequest(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        return null;
    }
}
