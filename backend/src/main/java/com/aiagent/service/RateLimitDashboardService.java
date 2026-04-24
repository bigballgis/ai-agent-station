package com.aiagent.service;

import com.aiagent.util.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 速率限制仪表盘服务
 *
 * 聚合 Redis 中的速率限制指标，为仪表盘提供统计视图。
 * 数据来源: Redis 中以 "rl:" 为前缀的速率限制计数器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitDashboardService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rl:";
    private static final String TENANT_PREFIX = "tenant:";
    private static final String IP_PREFIX = "ip:";

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

    /**
     * 获取速率限制仪表盘统计数据
     *
     * @return 包含总请求数、限流命中数、Top 限流端点、各租户用量等信息的 Map
     */
    public Map<String, Object> getRateLimitDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            Set<String> allKeys = redisTemplate.keys(RATE_LIMIT_PREFIX + "*");
            if (allKeys == null || allKeys.isEmpty()) {
                stats.put("totalRequestsInWindow", 0);
                stats.put("rateLimitHits", 0);
                stats.put("topRateLimitedEndpoints", Collections.emptyList());
                stats.put("perTenantUsage", Collections.emptyList());
                stats.put("windowSeconds", 60);
                stats.put("collectedAt", LocalDateTime.now().toString());
                return stats;
            }

            // 1. 统计总请求数和限流命中数
            int totalRequests = 0;
            int rateLimitHits = 0;
            Map<String, EndpointRateLimitInfo> endpointStats = new HashMap<>();
            Map<String, TenantRateLimitInfo> tenantStats = new HashMap<>();

            for (String key : allKeys) {
                String countStr = redisTemplate.opsForValue().get(key);
                if (countStr == null) continue;

                int count;
                try {
                    count = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    continue;
                }

                totalRequests += count;

                // 解析 key: rl:tenant:{tenantId}:{endpointType} 或 rl:ip:{ip}:{endpointType}
                String keyWithoutPrefix = key.substring(RATE_LIMIT_PREFIX.length());
                String[] parts = keyWithoutPrefix.split(":", 3);
                if (parts.length < 3) continue;

                String dimension = parts[0]; // "tenant" or "ip"
                String dimensionValue = parts[1]; // tenantId or ip
                String endpointType = parts[2];

                int limit = ENDPOINT_TYPE_DEFAULTS.getOrDefault(endpointType, 100);

                // 统计超限次数
                if (count > limit) {
                    rateLimitHits += (count - limit);
                }

                // 汇总端点统计
                EndpointRateLimitInfo endpointInfo = endpointStats.computeIfAbsent(
                        endpointType, k -> new EndpointRateLimitInfo(endpointType, 0, 0, limit));
                endpointInfo.totalRequests += count;
                if (count > limit) {
                    endpointInfo.rateLimitHits += (count - limit);
                }

                // 汇总租户统计（仅统计 tenant 维度）
                if ("tenant".equals(dimension)) {
                    TenantRateLimitInfo tenantInfo = tenantStats.computeIfAbsent(
                            dimensionValue, k -> new TenantRateLimitInfo(dimensionValue, 0, 0));
                    tenantInfo.totalRequests += count;
                    if (count > limit) {
                        tenantInfo.rateLimitHits += (count - limit);
                    }
                }
            }

            stats.put("totalRequestsInWindow", totalRequests);
            stats.put("rateLimitHits", rateLimitHits);

            // 2. Top 限流端点（按限流命中数降序）
            List<Map<String, Object>> topEndpoints = endpointStats.values().stream()
                    .filter(e -> e.rateLimitHits > 0)
                    .sorted(Comparator.comparingInt((EndpointRateLimitInfo e) -> e.rateLimitHits).reversed())
                    .limit(5)
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("endpointType", e.endpointType);
                        m.put("totalRequests", e.totalRequests);
                        m.put("rateLimitHits", e.rateLimitHits);
                        m.put("limit", e.limit);
                        m.put("usagePercent", e.limit > 0
                                ? Math.round(e.totalRequests * 100.0 / e.limit * 10) / 10.0
                                : 0);
                        return m;
                    })
                    .collect(Collectors.toList());
            stats.put("topRateLimitedEndpoints", topEndpoints);

            // 3. 各租户速率限制用量
            List<Map<String, Object>> perTenantUsage = tenantStats.values().stream()
                    .sorted(Comparator.comparingInt((TenantRateLimitInfo t) -> t.totalRequests).reversed())
                    .limit(10)
                    .map(t -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("tenantId", t.tenantId);
                        m.put("totalRequests", t.totalRequests);
                        m.put("rateLimitHits", t.rateLimitHits);
                        return m;
                    })
                    .collect(Collectors.toList());
            stats.put("perTenantUsage", perTenantUsage);

            stats.put("windowSeconds", 60);
            stats.put("collectedAt", LocalDateTime.now().toString());

        } catch (Exception e) {
            log.warn("获取速率限制仪表盘统计失败: {}", e.getMessage());
            stats.put("totalRequestsInWindow", 0);
            stats.put("rateLimitHits", 0);
            stats.put("topRateLimitedEndpoints", Collections.emptyList());
            stats.put("perTenantUsage", Collections.emptyList());
            stats.put("windowSeconds", 60);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * 获取指定租户的速率限制使用情况
     */
    public Map<String, Object> getTenantRateLimitUsage(Long tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();
        String tenantKeyPrefix = RATE_LIMIT_PREFIX + TENANT_PREFIX + tenantId + ":";

        try {
            Set<String> tenantKeys = redisTemplate.keys(tenantKeyPrefix + "*");
            if (tenantKeys == null || tenantKeys.isEmpty()) {
                result.put("tenantId", tenantId);
                result.put("endpoints", Collections.emptyList());
                return result;
            }

            List<Map<String, Object>> endpoints = new ArrayList<>();
            for (String key : tenantKeys) {
                String endpointType = key.substring(tenantKeyPrefix.length());
                String countStr = redisTemplate.opsForValue().get(key);
                int count = countStr != null ? Integer.parseInt(countStr) : 0;
                int limit = ENDPOINT_TYPE_DEFAULTS.getOrDefault(endpointType, 100);

                Map<String, Object> ep = new LinkedHashMap<>();
                ep.put("endpointType", endpointType);
                ep.put("used", count);
                ep.put("limit", limit);
                ep.put("remaining", Math.max(0, limit - count));
                ep.put("usagePercent", limit > 0
                        ? Math.round(count * 100.0 / limit * 10) / 10.0 : 0);
                endpoints.add(ep);
            }

            result.put("tenantId", tenantId);
            result.put("endpoints", endpoints);
        } catch (Exception e) {
            log.warn("获取租户速率限制用量失败: tenantId={}, error={}", tenantId, e.getMessage());
            result.put("tenantId", tenantId);
            result.put("endpoints", Collections.emptyList());
            result.put("error", e.getMessage());
        }

        return result;
    }

    // ==================== 内部数据类 ====================

    @Data
    private static class EndpointRateLimitInfo {
        final String endpointType;
        int totalRequests;
        int rateLimitHits;
        final int limit;

        EndpointRateLimitInfo(String endpointType, int totalRequests, int rateLimitHits, int limit) {
            this.endpointType = endpointType;
            this.totalRequests = totalRequests;
            this.rateLimitHits = rateLimitHits;
            this.limit = limit;
        }
    }

    @Data
    private static class TenantRateLimitInfo {
        final String tenantId;
        int totalRequests;
        int rateLimitHits;

        TenantRateLimitInfo(String tenantId, int totalRequests, int rateLimitHits) {
            this.tenantId = tenantId;
            this.totalRequests = totalRequests;
            this.rateLimitHits = rateLimitHits;
        }
    }
}
