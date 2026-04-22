package com.aiagent.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GatewayFilter, Ordered {

    public static final String TENANT_ID_ATTR = "tenantId";
    public static final String USER_ID_ATTR = "userId";
    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        
        // 从请求中提取认证信息
        String apiKey = headers.getFirst(API_KEY_HEADER);
        String authorization = headers.getFirst(AUTHORIZATION_HEADER);
        
        // 简单的认证逻辑（实际项目中应该调用用户服务验证）
        if (apiKey != null && !apiKey.isEmpty()) {
            // 从API Key中解析tenantId和userId（简化示例）
            Long tenantId = 1L; // 实际应从API Key服务获取
            Long userId = 1L;
            exchange.getAttributes().put(TENANT_ID_ATTR, tenantId);
            exchange.getAttributes().put(USER_ID_ATTR, userId);
            return chain.filter(exchange);
        } else if (authorization != null && authorization.startsWith("Bearer ")) {
            // JWT认证
            Long tenantId = 1L; // 实际应从JWT解析
            Long userId = 1L;
            exchange.getAttributes().put(TENANT_ID_ATTR, tenantId);
            exchange.getAttributes().put(USER_ID_ATTR, userId);
            return chain.filter(exchange);
        }
        
        // 没有认证信息，返回401
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
