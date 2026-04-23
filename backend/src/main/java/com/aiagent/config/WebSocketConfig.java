package com.aiagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.aiagent.websocket.NotificationWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationWebSocketHandler notificationHandler;

    @Value("${websocket.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String[] allowedOrigins;

    public WebSocketConfig(NotificationWebSocketHandler notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notifications")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            org.springframework.web.socket.WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            HttpServletRequest httpRequest = servletRequest.getServletRequest();
                            String token = httpRequest.getHeader("Authorization");
                            if (token != null && token.startsWith("Bearer ")) {
                                // 简单验证token存在性（实际应调用JwtUtil验证）
                                attributes.put("userId", "authenticated");
                                return true;
                            }
                        }
                        // 允许连接但标记为未认证（通知处理器可根据此过滤）
                        attributes.put("userId", "anonymous");
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            org.springframework.web.socket.WebSocketHandler wsHandler, Exception exception) {
                    }
                });
    }
}
