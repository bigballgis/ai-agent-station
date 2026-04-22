package com.aiagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一线程池配置
 * 
 * 最佳实践:
 * - 使用 ThreadPoolTaskExecutor 替代 Executors 工厂方法
 * - 所有线程池参数可通过配置文件调整
 * - 线程命名便于排查问题
 * - 核心线程数 = CPU 核数，最大线程数 = CPU 核数 * 2
 * - 队列容量有上限，防止 OOM
 * - 拒绝策略: CallerRunsPolicy（降级为同步执行，不丢弃任务）
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Value("${app.thread-pool.core-size:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}")
    private int coreSize;

    @Value("${app.thread-pool.max-size:#{T(java.lang.Runtime).getRuntime().availableProcessors() * 2}}")
    private int maxSize;

    @Value("${app.thread-pool.queue-capacity:200}")
    private int queueCapacity;

    @Value("${app.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${app.thread-pool.thread-name-prefix:ai-agent-}")
    private String threadNamePrefix;

    /**
     * 通用异步任务线程池
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("[ThreadPool] 初始化通用线程池: core={}, max={}, queue={}",
                coreSize, maxSize, queueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * SSE 流式输出专用线程池（IO 密集型，核心线程数更大）
     */
    @Bean(name = "sseExecutor")
    public Executor sseExecutor() {
        log.info("[ThreadPool] 初始化 SSE 线程池: core={}, max={}, queue={}",
                coreSize * 2, maxSize * 2, 500);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize * 2);
        executor.setMaxPoolSize(maxSize * 2);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("sse-stream-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Agent 执行专用线程池（CPU 密集型）
     */
    @Bean(name = "agentExecutor")
    public Executor agentExecutor() {
        log.info("[ThreadPool] 初始化 Agent 执行线程池: core={}, max={}, queue={}",
                coreSize, maxSize, 100);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("agent-exec-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
