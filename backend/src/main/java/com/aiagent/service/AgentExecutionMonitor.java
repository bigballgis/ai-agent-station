package com.aiagent.service;

import com.aiagent.config.properties.AiAgentProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Agent 执行监控服务
 *
 * 核心能力:
 * 1. 并发执行限制 - 每个 Agent 最多 maxConcurrentExecutions 个并发执行
 * 2. 执行超时控制 - 可配置超时时间，默认 2 分钟
 * 3. 错误率追踪 - 通过 Prometheus Counter 记录执行错误
 */
@Slf4j
@Service
public class AgentExecutionMonitor {

    private final StringRedisTemplate redisTemplate;
    private final Counter agentErrorCounter;
    private final AiAgentProperties aiAgentProperties;

    /** 每个 Agent 的并发信号量 */
    private final ConcurrentHashMap<Long, Semaphore> agentSemaphores = new ConcurrentHashMap<>();

    public AgentExecutionMonitor(StringRedisTemplate redisTemplate, MeterRegistry registry,
                                  AiAgentProperties aiAgentProperties) {
        this.redisTemplate = redisTemplate;
        this.aiAgentProperties = aiAgentProperties;
        this.agentErrorCounter = Counter.builder("agent_execution_errors_total")
                .description("Total number of agent execution errors")
                .tag("application", "ai-agent-platform")
                .register(registry);
    }

    /**
     * 尝试获取执行许可
     *
     * @param agentId Agent ID
     * @return true 如果获取成功，false 如果并发数已达上限
     */
    public boolean tryAcquireExecution(Long agentId) {
        int maxConcurrentExecutions = aiAgentProperties.getExecution().getMaxConcurrentPerAgent();
        Semaphore semaphore = agentSemaphores.computeIfAbsent(agentId,
                id -> new Semaphore(maxConcurrentExecutions));
        boolean acquired = semaphore.tryAcquire();
        if (!acquired) {
            log.warn("Agent {} 并发执行数已达上限 {}，拒绝执行", agentId, maxConcurrentExecutions);
        }
        return acquired;
    }

    /**
     * 释放执行许可
     *
     * @param agentId Agent ID
     */
    public void releaseExecution(Long agentId) {
        Semaphore semaphore = agentSemaphores.get(agentId);
        if (semaphore != null) {
            semaphore.release();
        }
    }

    /**
     * 记录执行错误
     *
     * @param agentId Agent ID
     * @param errorType 错误类型
     */
    public void recordError(Long agentId, String errorType) {
        agentErrorCounter.increment();
        log.debug("Agent {} 执行错误: {}", agentId, errorType);
    }

    /**
     * 获取当前 Agent 的并发执行数
     *
     * @param agentId Agent ID
     * @return 当前并发执行数
     */
    public int getCurrentConcurrency(Long agentId) {
        int maxConcurrentExecutions = aiAgentProperties.getExecution().getMaxConcurrentPerAgent();
        Semaphore semaphore = agentSemaphores.get(agentId);
        if (semaphore == null) {
            return 0;
        }
        return maxConcurrentExecutions - semaphore.availablePermits();
    }

    /**
     * 获取执行超时时间（秒）
     */
    public int getExecutionTimeoutSeconds() {
        return aiAgentProperties.getExecution().getTimeoutSeconds();
    }

    /**
     * 获取最大并发执行数
     */
    public int getMaxConcurrentExecutions() {
        return aiAgentProperties.getExecution().getMaxConcurrentPerAgent();
    }
}
