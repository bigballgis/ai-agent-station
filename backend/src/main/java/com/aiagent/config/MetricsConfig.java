package com.aiagent.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prometheus 自定义业务指标配置
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter agentInvocationsTotal(MeterRegistry registry) {
        return Counter.builder("agent_invocations_total")
                .description("Total number of agent invocations")
                .tag("application", "ai-agent-platform")
                .register(registry);
    }

    @Bean
    public Timer agentExecutionDurationSeconds(MeterRegistry registry) {
        return Timer.builder("agent_execution_duration_seconds")
                .description("Duration of agent executions in seconds")
                .tag("application", "ai-agent-platform")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public AtomicInteger activeUsersGauge(MeterRegistry registry) {
        AtomicInteger activeUsers = new AtomicInteger(0);
        registry.gauge("active_users_gauge", activeUsers);
        return activeUsers;
    }
}
