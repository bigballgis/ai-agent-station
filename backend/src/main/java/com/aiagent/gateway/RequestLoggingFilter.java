package com.aiagent.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestLoggingFilter implements GatewayFilter, Ordered {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTR = "requestId";
    public static final String START_TIME_ATTR = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        exchange.getAttributes().put(REQUEST_ID_ATTR, requestId);
        exchange.getAttributes().put(START_TIME_ATTR, startTime);

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .doFinally(signal -> {
                    long endTime = System.currentTimeMillis();
                    long executionTime = endTime - startTime;
                    exchange.getAttributes().put("executionTime", executionTime);
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
