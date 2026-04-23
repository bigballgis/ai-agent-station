package com.aiagent.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class AuthenticationFilter implements GatewayFilter, Ordered {

    public static final String TENANT_ID_ATTR = "tenantId";
    public static final String USER_ID_ATTR = "userId";
    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String apiKey = headers.getFirst(API_KEY_HEADER);
        String authorization = headers.getFirst(AUTHORIZATION_HEADER);

        if (apiKey != null && !apiKey.isEmpty()) {
            // API Key认证 - 从header中提取tenant信息
            // 注意: 生产环境应调用API Key服务验证
            String tenantIdHeader = headers.getFirst("X-Tenant-ID");
            Long tenantId = tenantIdHeader != null ? Long.parseLong(tenantIdHeader) : null;
            if (tenantId == null) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }
            exchange.getAttributes().put(TENANT_ID_ATTR, tenantId);
            return chain.filter(exchange);
        } else if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token = authorization.substring(7);
                var claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                Long tenantId = claims.get("tenantId", Long.class);
                Long userId = claims.get("userId", Long.class);
                if (tenantId != null) {
                    exchange.getAttributes().put(TENANT_ID_ATTR, tenantId);
                    exchange.getAttributes().put(USER_ID_ATTR, userId);
                    return chain.filter(exchange);
                }
            } catch (Exception e) {
                // Token无效
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
