package com.aiagent.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Agent调用路由
                .route("agent-execution", r -> r
                        .path("/api/v1/agent/{agentId}/invoke")
                        .and()
                        .method("POST")
                        .filters(f -> f
                                .filter(new RequestLoggingFilter())
                                .filter(new AuthenticationFilter())
                                .filter(new RateLimitFilter())
                                .stripPrefix(0)
                        )
                        .uri("http://localhost:8080"))
                // Agent状态查询路由
                .route("agent-status", r -> r
                        .path("/api/v1/agent/{agentId}/status")
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .filter(new RequestLoggingFilter())
                                .filter(new AuthenticationFilter())
                                .stripPrefix(0)
                        )
                        .uri("http://localhost:8080"))
                // 异步任务查询路由
                .route("async-task", r -> r
                        .path("/api/v1/task/{taskId}")
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .filter(new RequestLoggingFilter())
                                .filter(new AuthenticationFilter())
                                .stripPrefix(0)
                        )
                        .uri("http://localhost:8080"))
                .build();
    }
}
