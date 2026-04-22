package com.aiagent.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j 容错配置
 *
 * 为 LLM API 调用提供三层防护:
 * 1. Retry — 失败自动重试（指数退避），应对瞬时故障
 * 2. Circuit Breaker — 连续失败时熔断，防止级联故障
 * 3. Rate Limiter — 令牌桶限流，防止 API 费用失控
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    // ==================== LLM 调用熔断器 ====================

    @Bean
    public CircuitBreaker llmCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                    // 50% 失败率触发熔断
                .slowCallRateThreshold(80)                   // 80% 慢调用触发熔断
                .slowCallDurationThreshold(Duration.ofSeconds(30)) // 超过30s算慢调用
                .waitDurationInOpenState(Duration.ofSeconds(60))   // 熔断后等待60s
                .permittedNumberOfCallsInHalfOpenState(3)    // 半开状态允许3次试探
                .minimumNumberOfCalls(5)                     // 最少5次调用才计算失败率
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)                      // 滑动窗口大小10
                .build();

        CircuitBreaker cb = CircuitBreaker.of("llm", config);

        cb.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("[Resilience4j] LLM 熔断器状态变更: {}", event))
                .onError(event ->
                        log.warn("[Resilience4j] LLM 调用失败: {}", event.getThrowable().getMessage()));

        return cb;
    }

    // ==================== LLM 调用重试器 ====================

    @Bean
    public Retry llmRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                              // 最多重试3次
                .waitDuration(Duration.ofSeconds(1))         // 初始等待1s
                .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
                .retryOnException(e ->                       // 仅对可重试异常重试
                        isRetryableException(e))
                .ignoreExceptions(
                        IllegalArgumentException.class,      // 参数错误不重试
                        org.springframework.security.access.AccessDeniedException.class) // 权限错误不重试
                .build();

        Retry retry = Retry.of("llm", config);

        retry.getEventPublisher()
                .onRetry(event ->
                        log.info("[Resilience4j] LLM 重试 #{}: {}", event.getNumberOfRetryAttempts(),
                                event.getLastThrowable().getMessage()))
                .onError(event ->
                        log.error("[Resilience4j] LLM 重试耗尽: {}", event.getLastThrowable().getMessage()));

        return retry;
    }

    // ==================== LLM 调用限流器 ====================

    @Bean
    public RateLimiter llmRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(60)                         // 每周期60个令牌
                .limitRefreshPeriod(Duration.ofMinutes(1))  // 周期1分钟
                .timeoutDuration(Duration.ofSeconds(5))     // 等待令牌超时5s
                .build();

        RateLimiter rl = RateLimiter.of("llm", config);

        rl.getEventPublisher()
                .onSuccess(event ->
                        log.debug("[Resilience4j] LLM 限流请求通过: permits={}", event.getNumberOfPermits()))
                .onFailure(event ->
                        log.warn("[Resilience4j] LLM 请求被限流"));

        return rl;
    }

    /**
     * 判断异常是否可重试
     */
    private boolean isRetryableException(Throwable e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg == null) return true;

        // 网络超时、连接拒绝等可重试
        if (msg.contains("timeout") || msg.contains("Timed out") ||
            msg.contains("Connection refused") || msg.contains("Connection reset") ||
            msg.contains("SocketException") || msg.contains("SSLException")) {
            return true;
        }

        // LLM API 429 (Rate Limit) 和 5xx 可重试
        if (msg.contains("429") || msg.contains("500") || msg.contains("502") ||
            msg.contains("503") || msg.contains("rate_limit")) {
            return true;
        }

        // 认证错误不重试
        if (msg.contains("401") || msg.contains("403") || msg.contains("invalid_api_key")) {
            return false;
        }

        return true; // 默认重试
    }
}
