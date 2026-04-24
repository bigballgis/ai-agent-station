package com.aiagent.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置
 *
 * 性能优化:
 * - 实现 AsyncConfigurer 接口，提供 Spring 默认异步执行器
 * - 核心线程数 5，最大线程数 20，队列容量 100
 * - CallerRunsPolicy 拒绝策略：队列满时降级为同步执行，不丢弃任务
 * - 线程命名前缀 "async-" 便于日志排查
 *
 * 注意：项目中已有 ThreadPoolConfig 提供了更细粒度的线程池配置
 * （taskExecutor, sseExecutor, agentExecutor），建议优先使用具名线程池。
 * 此配置作为 Spring @Async 默认执行器的兜底配置。
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
