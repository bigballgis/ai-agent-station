package com.aiagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 配置
 *
 * 启用 @Retryable 注解支持，为服务层方法提供声明式重试能力。
 * 与 Resilience4j 互补：Resilience4j 用于 LLM 调用的编程式容错，
 * Spring Retry 用于通用服务方法的注解式重试。
 */
@Slf4j
@Configuration
@EnableRetry
public class RetryConfig {

    public RetryConfig() {
        log.info("[RetryConfig] Spring Retry 已启用 (@Retryable 注解支持)");
    }
}
