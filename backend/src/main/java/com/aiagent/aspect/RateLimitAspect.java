package com.aiagent.aspect;

import com.aiagent.annotation.RateLimit;
import com.aiagent.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RateLimit 注解 AOP 切面
 * <p>
 * 基于内存的滑动窗口限流实现，适用于单实例部署。
 * 使用 ConcurrentHashMap + AtomicInteger 实现线程安全的计数器。
 * </p>
 *
 * TODO: 集群部署时，应替换为基于 Redis 的分布式限流方案（如 Redis + Lua 脚本）
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /**
     * 限流计数器: key = "method:maxRequests:windowSeconds:clientIP"
     * value = { windowStartTimestamp, requestCount }
     */
    private final Map<String, WindowCounter> counterMap = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String clientIp = getClientIp();
        String methodSignature = joinPoint.getSignature().toShortString();
        String key = methodSignature + ":" + rateLimit.maxRequests() + ":" + rateLimit.windowSeconds() + ":" + clientIp;

        int maxRequests = rateLimit.maxRequests();
        int windowSeconds = rateLimit.windowSeconds();
        long windowStart = System.currentTimeMillis() / 1000 / windowSeconds * windowSeconds;

        WindowCounter counter = counterMap.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart != windowStart) {
                return new WindowCounter(windowStart);
            }
            return existing;
        });

        int currentCount = counter.incrementAndGet();
        if (currentCount > maxRequests) {
            log.warn("[RateLimit] 请求被限流: method={}, ip={}, count={}/{}, window={}s",
                    methodSignature, clientIp, currentCount, maxRequests, windowSeconds);
            throw new RateLimitException("请求过于频繁，请在 " + windowSeconds + " 秒后重试");
        }

        return joinPoint.proceed();
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 滑动窗口计数器
     */
    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
