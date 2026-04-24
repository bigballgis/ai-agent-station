package com.aiagent.service;

import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.entity.RateLimitConfig;
import com.aiagent.exception.RateLimitExceededException;
import com.aiagent.repository.RateLimitConfigRepository;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 速率限制服务
 *
 * 核心能力:
 * 1. 基于 Redis 的分布式限流（INCR + EXPIRE 模式）
 * 2. 支持按租户 (tenantId) 维度限流
 * 3. 支持按端点类型配置不同的限流规则
 * 4. 提供限流状态查询接口
 *
 * 限流维度优先级:
 * 1. 数据库配置（rate_limit_configs 表，按租户+端点类型）
 * 2. 应用配置（application.yml 中的默认值）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitConfigRepository rateLimitConfigRepository;
    private final AiAgentProperties aiAgentProperties;

    private static final String RATE_LIMIT_PREFIX = "rl:";
    private static final String TENANT_PREFIX = "tenant:";
    private static final String IP_PREFIX = "ip:";

    // ==================== 默认限流配置（可通过 application.yml 覆盖） ====================

    private int getDefaultLimitPerMinute() {
        return aiAgentProperties.getRateLimit().getApi().getDefaultLimitPerMinute();
    }

    private int getDefaultLimitPerHour() {
        return aiAgentProperties.getRateLimit().getApi().getDefaultLimitPerHour();
    }

    private int getDefaultBurstCapacity() {
        return aiAgentProperties.getRateLimit().getApi().getBurstCapacity();
    }

    // ==================== 端点类型限流配置 ====================

    /** 不同端点类型的默认限流规则 (requests per minute) */
    private static final Map<String, Integer> ENDPOINT_TYPE_DEFAULTS = Map.of(
            "AUTH", 30,
            "AGENT_INVOKE", 60,
            "AGENT_QUERY", 120,
            "FILE_UPLOAD", 20,
            "FILE_DOWNLOAD", 60,
            "DATA_EXPORT", 10,
            "API_GATEWAY", 100,
            "SYSTEM", 60,
            "DEFAULT", 100
    );

    // ==================== 限流检查 ====================

    /**
     * 检查 API 速率限制（按租户维度）
     *
     * @param endpointType 端点类型（如 AUTH, AGENT_INVOKE 等）
     * @param clientIp     客户端 IP
     * @throws RateLimitExceededException 超出限制时抛出
     */
    public void checkRateLimit(String endpointType, String clientIp) {
        Long tenantId = TenantContextHolder.getTenantId();
        int limitPerMinute = getEffectiveLimit(endpointType, tenantId);

        // 优先按租户限流，回退到按 IP 限流
        String key;
        if (tenantId != null) {
            key = RATE_LIMIT_PREFIX + TENANT_PREFIX + tenantId + ":" + endpointType;
        } else {
            key = RATE_LIMIT_PREFIX + IP_PREFIX + clientIp + ":" + endpointType;
        }

        RateLimitResult result = checkAndIncrement(key, limitPerMinute, 60);

        if (!result.allowed) {
            log.warn("[RateLimitService] 速率限制触发: type={}, tenant={}, ip={}, count={}/{}, resetTime={}",
                    endpointType, tenantId, clientIp, result.count, limitPerMinute, result.resetTime);
            throw new RateLimitExceededException(
                    limitPerMinute, 0, result.resetTime, result.windowSeconds);
        }
    }

    /**
     * 检查速率限制并获取当前状态（不抛异常）
     *
     * @param endpointType 端点类型
     * @param clientIp     客户端 IP
     * @return 限流状态
     */
    public RateLimitStatus getRateLimitStatus(String endpointType, String clientIp) {
        Long tenantId = TenantContextHolder.getTenantId();
        int limitPerMinute = getEffectiveLimit(endpointType, tenantId);

        String key;
        if (tenantId != null) {
            key = RATE_LIMIT_PREFIX + TENANT_PREFIX + tenantId + ":" + endpointType;
        } else {
            key = RATE_LIMIT_PREFIX + IP_PREFIX + clientIp + ":" + endpointType;
        }

        String countStr = redisTemplate.opsForValue().get(key);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        int remaining = Math.max(0, limitPerMinute - currentCount);
        long resetTime = System.currentTimeMillis() / 1000 + 60; // 估算重置时间

        return new RateLimitStatus(
                endpointType,
                limitPerMinute,
                remaining,
                currentCount,
                resetTime,
                tenantId
        );
    }

    /**
     * 获取所有端点类型的限流状态
     */
    public List<RateLimitStatus> getAllRateLimitStatuses(String clientIp) {
        List<RateLimitStatus> statuses = new ArrayList<>();
        for (String endpointType : ENDPOINT_TYPE_DEFAULTS.keySet()) {
            statuses.add(getRateLimitStatus(endpointType, clientIp));
        }
        return statuses;
    }

    // ==================== 内部方法 ====================

    /**
     * 获取有效的限流值
     * 优先级: 数据库配置 > 端点类型默认值 > 全局默认值
     */
    private int getEffectiveLimit(String endpointType, Long tenantId) {
        // 1. 尝试从数据库获取租户级配置
        if (tenantId != null) {
            Optional<RateLimitConfig> configOpt = rateLimitConfigRepository
                    .findByTenantIdAndLimitTypeAndIsActiveTrue(tenantId, endpointType);
            if (configOpt.isPresent()) {
                return configOpt.get().getRequestsPerMinute();
            }
        }

        // 2. 使用端点类型默认值
        return ENDPOINT_TYPE_DEFAULTS.getOrDefault(endpointType, getDefaultLimitPerMinute());
    }

    /**
     * Redis 原子递增 + 检查限流
     */
    private RateLimitResult checkAndIncrement(String key, int maxRequests, int windowSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);

            if (count == null) {
                // Redis 不可用时降级放行
                log.warn("[RateLimitService] Redis INCR 返回 null，降级放行: key={}", key);
                return new RateLimitResult(true, 0, windowSeconds, windowSeconds);
            }

            // 首次请求时设置过期时间
            if (count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            long resetTime = System.currentTimeMillis() / 1000 + windowSeconds;
            boolean allowed = count <= maxRequests;

            return new RateLimitResult(allowed, count.intValue(), resetTime, windowSeconds);
        } catch (Exception e) {
            // Redis 异常时降级放行
            log.warn("[RateLimitService] Redis 异常，降级放行: key={}, error={}", key, e.getMessage());
            return new RateLimitResult(true, 0, windowSeconds, windowSeconds);
        }
    }

    // ==================== 内部数据类 ====================

    @Data
    private static class RateLimitResult {
        final boolean allowed;
        final int count;
        final long resetTime;
        final int windowSeconds;

        RateLimitResult(boolean allowed, int count, long resetTime, int windowSeconds) {
            this.allowed = allowed;
            this.count = count;
            this.resetTime = resetTime;
            this.windowSeconds = windowSeconds;
        }
    }

    /**
     * 限流状态（对外暴露）
     */
    @Data
    public static class RateLimitStatus {
        /** 端点类型 */
        private final String endpointType;
        /** 窗口内最大请求数 */
        private final int limit;
        /** 剩余请求数 */
        private final int remaining;
        /** 当前已使用请求数 */
        private final int used;
        /** 重置时间（Unix 时间戳，秒） */
        private final long resetTime;
        /** 租户 ID（可能为 null） */
        private final Long tenantId;
    }
}
