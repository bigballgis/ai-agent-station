package com.aiagent.aspect;

import com.aiagent.annotation.RateLimit;
import com.aiagent.exception.RateLimitExceededException;
import com.aiagent.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * RateLimit 注解 AOP 切面
 * <p>
 * 基于 Redis 的分布式限流实现，使用 INCR + EXPIRE 模式。
 * 支持按 IP + 方法签名 或 租户 + 方法签名 作为 key，支持集群部署。
 * </p>
 * <p>
 * 限流维度优先级:
 * 1. 如果当前请求有租户上下文 (TenantContextHolder)，按租户+方法签名限流
 * 2. 否则按 IP+方法签名限流
 * </p>
 * <p>
 * 响应头:
 * - X-RateLimit-Limit: 窗口内最大请求数
 * - X-RateLimit-Remaining: 剩余请求数
 * - X-RateLimit-Reset: 重置时间（Unix 时间戳，秒）
 * </p>
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private final StringRedisTemplate redisTemplate;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        String clientIp = getClientIp();
        String methodSignature = joinPoint.getSignature().toShortString();
        int maxRequests = rateLimit.maxRequests();
        int windowSeconds = rateLimit.windowSeconds();

        // 优先按租户限流，回退到按 IP 限流
        String key;
        if (tenantId != null) {
            key = RATE_LIMIT_PREFIX + "tenant:" + tenantId + ":" + methodSignature;
        } else {
            key = RATE_LIMIT_PREFIX + "ip:" + clientIp + ":" + methodSignature;
        }

        try {
            // 使用 Redis INCR 原子递增
            Long count = redisTemplate.opsForValue().increment(key);

            if (count == null) {
                // Redis 不可用时放行（降级策略）
                log.warn("[RateLimit] Redis INCR 返回 null，降级放行: method={}, ip={}, tenant={}",
                        methodSignature, clientIp, tenantId);
                return joinPoint.proceed();
            }

            // 第一次请求时设置过期时间
            if (count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            int remaining = Math.max(0, maxRequests - count.intValue());
            long resetTime = System.currentTimeMillis() / 1000 + windowSeconds;

            // 写入速率限制响应头
            addRateLimitHeaders(maxRequests, remaining, resetTime);

            if (count > maxRequests) {
                log.warn("[RateLimit] 请求被限流: method={}, ip={}, tenant={}, count={}/{}, window={}s",
                        methodSignature, clientIp, tenantId, count, maxRequests, windowSeconds);
                throw new RateLimitExceededException(
                        "请求过于频繁，请在 " + windowSeconds + " 秒后重试",
                        maxRequests, 0, resetTime, windowSeconds);
            }
        } catch (RateLimitExceededException e) {
            // 限流异常直接抛出（已包含响应头信息，由 GlobalExceptionHandler 写入）
            throw e;
        } catch (Exception e) {
            // Redis 连接异常等不可恢复错误时降级放行
            log.warn("[RateLimit] Redis 异常，降级放行: method={}, ip={}, tenant={}, error={}",
                    methodSignature, clientIp, tenantId, e.getMessage());
        }

        return joinPoint.proceed();
    }

    /**
     * 向当前响应写入速率限制头
     */
    private void addRateLimitHeaders(int limit, int remaining, long resetTime) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;

        HttpServletResponse response = attributes.getResponse();
        if (response == null) return;

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
    }

    private String getClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
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
}
