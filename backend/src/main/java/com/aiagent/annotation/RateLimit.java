package com.aiagent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * <p>
 * 基于 AOP 切面实现的简易限流，使用内存中的滑动窗口计数器。
 * 适用于单实例部署场景，集群环境建议使用 Redis + RateLimiterFilter。
 * </p>
 *
 * 使用示例:
 * <pre>
 *     @RateLimit(maxRequests = 10, windowSeconds = 60)
 *     @GetMapping("/api/resource")
 *     public ResponseEntity<?> getResource() { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 时间窗口内允许的最大请求数
     */
    int maxRequests() default 10;

    /**
     * 时间窗口大小（秒）
     */
    int windowSeconds() default 60;
}
