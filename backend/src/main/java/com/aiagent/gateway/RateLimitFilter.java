package com.aiagent.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimitFilter implements GatewayFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RateLimitFilter() {
        this.redisTemplate = null;
    }

    public RateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (redisTemplate == null) {
            return chain.filter(exchange);
        }
        Long tenantId = exchange.getAttribute(AuthenticationFilter.TENANT_ID_ATTR);
        String agentId = extractAgentId(exchange);
        String rateLimitKey = "rate_limit:" + tenantId + ":" + agentId;

        return checkRateLimit(rateLimitKey)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                });
    }

    private Mono<Boolean> checkRateLimit(String key) {
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, Duration.ofSeconds(60))
                                .thenReturn(true);
                    }
                    return Mono.just(count <= 100); // 每分钟最多100次请求
                })
                .defaultIfEmpty(true);
    }

    private String extractAgentId(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length; i++) {
            if ("agent".equals(segments[i]) && i + 1 < segments.length) {
                return segments[i + 1];
            }
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -80;
    }
}
