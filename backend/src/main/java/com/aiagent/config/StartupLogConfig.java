package com.aiagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 应用启动日志增强 - 打印关键配置信息（不打印敏感值）
 */
@Slf4j
@Component
public class StartupLogConfig {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${jwt.expiration:1800000}")
    private long jwtExpiration;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String corsOrigins;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("==========================================================");
        log.info("  AI Agent Platform - Startup Configuration");
        log.info("==========================================================");
        log.info("  Active Profile    : {}", activeProfile);
        log.info("  Server Port       : {}", serverPort);
        log.info("  Database URL      : {}", maskPassword(datasourceUrl));
        log.info("  Redis Host        : {}:{}", redisHost, redisPort);
        log.info("  JWT Expiration    : {}ms ({}min)", jwtExpiration, jwtExpiration / 60000);
        log.info("  CORS Origins      : {}", formatOrigins(corsOrigins));
        log.info("==========================================================");
    }

    /**
     * 隐藏数据库 URL 中的密码
     */
    private String maskPassword(String url) {
        if (url == null || url.isEmpty()) {
            return "(not configured)";
        }
        // 匹配 jdbc:postgresql://user:password@host 或类似格式
        return url.replaceAll("(?<=:)([^:@/]+)(?=@)", "****");
    }

    /**
     * 格式化 CORS Origins，最多显示 5 个
     */
    private String formatOrigins(String origins) {
        if (origins == null || origins.isEmpty()) {
            return "(not configured)";
        }
        String[] parts = origins.split(",");
        if (parts.length <= 5) {
            return origins;
        }
        return Arrays.stream(Arrays.copyOf(parts, 5))
                .collect(Collectors.joining(", "))
                + String.format(" ... (and %d more)", parts.length - 5);
    }
}
