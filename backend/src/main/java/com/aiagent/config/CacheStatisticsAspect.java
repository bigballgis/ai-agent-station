package com.aiagent.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

/**
 * 缓存统计切面
 *
 * 通过 AOP 拦截 @CacheEvict 注解的方法，自动记录缓存驱逐统计。
 * 缓存命中/未命中统计由 Spring Cache 内部的 CacheInterceptor 处理，
 * 可通过 actuator /metrics 端点获取。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheStatisticsAspect {

    private final com.aiagent.service.CacheStatisticsService cacheStatisticsService;

    /**
     * 拦截 @CacheEvict 注解的方法，记录缓存驱逐统计
     */
    @Around("@annotation(cacheEvict)")
    public Object trackCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        String[] cacheNames = cacheEvict.value();
        try {
            return joinPoint.proceed();
        } finally {
            for (String cacheName : cacheNames) {
                cacheStatisticsService.recordEviction(cacheName);
            }
        }
    }
}
