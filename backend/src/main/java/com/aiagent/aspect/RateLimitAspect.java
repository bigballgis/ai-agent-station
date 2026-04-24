package com.aiagent.aspect;

import com.aiagent.annotation.RateLimit;
import com.aiagent.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
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
 * 按 IP + 方法签名作为 key，支持集群部署。
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
        String clientIp = getClientIp();
        String methodSignature = joinPoint.getSignature().toShortString();
        int maxRequests = rateLimit.maxRequests();
        int windowSeconds = rateLimit.windowSeconds();

        // key = rate_limit:{methodSignature}:{clientIp}
        String key = RATE_LIMIT_PREFIX + methodSignature + ":" + clientIp;

        try {
            // 使用 Redis INCR 原子递增
            Long count = redisTemplate.opsForValue().increment(key);

            if (count == null) {
                // Redis 不可用时放行（降级策略）
                log.warn("[RateLimit] Redis INCR 返回 null，降级放行: method={}, ip={}", methodSignature, clientIp);
                return joinPoint.proceed();
            }

            // 第一次请求时设置过期时间
            if (count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            if (count > maxRequests) {
                log.warn("[RateLimit] 请求被限流: method={}, ip={}, count={}/{}, window={}s",
                        methodSignature, clientIp, count, maxRequests, windowSeconds);
                throw new RateLimitException("请求过于频繁，请在 " + windowSeconds + " 秒后重试");
            }
        } catch (RateLimitException e) {
            // 限流异常直接抛出
            throw e;
        } catch (Exception e) {
            // Redis 连接异常等不可恢复错误时降级放行
            log.warn("[RateLimit] Redis 异常，降级放行: method={}, ip={}, error={}",
                    methodSignature, clientIp, e.getMessage());
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
}
