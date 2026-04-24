package com.aiagent.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * WebSocket 配置属性
 *
 * 对应 application.yml 中 websocket.* 前缀的配置项。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    /** WebSocket 允许的跨域来源 */
    private String[] allowedOrigins = {"http://localhost:5173", "http://localhost:3000"};
}
