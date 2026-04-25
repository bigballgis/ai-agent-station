package com.aiagent.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 响应时间 Prometheus 直方图配置
 * 按方法、URI、状态码记录每个端点的响应时间分布
 */
@Configuration
public class ApiResponseTimeMetrics {

    /**
     * 注册 API 响应时间直方图
     * 指标名: http_server_requests_seconds
     * 标签: method, uri, status
     * 桶: 0.1, 0.25, 0.5, 1, 2.5, 5, 10 秒
     */
    @Bean
    public Timer httpResponseTimeTimer(MeterRegistry registry) {
        return Timer.builder("http_server_requests_seconds")
                .description("API response time in seconds")
                .tags("application", "aegisnexus")
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        java.time.Duration.ofMillis(100),
                        java.time.Duration.ofMillis(250),
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofSeconds(1),
                        java.time.Duration.ofSeconds(2),
                        java.time.Duration.ofSeconds(5),
                        java.time.Duration.ofSeconds(10)
                )
                .register(registry);
    }
}
