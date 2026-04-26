package com.aiagent.service;

import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 综合应用健康检查服务
 *
 * 聚合所有组件的健康状态，提供统一的健康检查视图:
 * - 数据库连接 (PostgreSQL)
 * - Redis 连接
 * - 磁盘空间
 * - 外部 LLM 提供商连通性
 * - JWT 密钥有效性
 * - 缓存命中率监控
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationHealthService {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final CacheStatisticsService cacheStatisticsService;
    private final JwtUtil jwtUtil;
    private final AiAgentProperties aiAgentProperties;

    private static final double CACHE_HIT_RATE_WARNING_THRESHOLD = 50.0;
    private static final long DISK_SPACE_THRESHOLD_MB = 500;
    private static final Duration LLM_CHECK_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 获取所有组件的综合健康状态
     */
    public Map<String, Object> getDetailedHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        Instant checkTime = Instant.now();

        // 关键依赖检查
        health.put("database", checkDatabaseHealth());
        health.put("redis", checkRedisHealth());
        health.put("diskSpace", checkDiskSpaceHealth());

        // 应用层检查
        health.put("llmProvider", checkLlmProviderHealth());
        health.put("jwtSecret", checkJwtSecretHealth());
        health.put("cacheHitRate", checkCacheHitRateHealth());

        // 汇总状态
        boolean allUp = health.values().stream()
                .allMatch(v -> v instanceof Map && "UP".equals(((Map<?, ?>) v).get("status")));
        health.put("overallStatus", allUp ? "UP" : "DEGRADED");
        health.put("checkTime", checkTime.toString());

        return health;
    }

    /**
     * 仅检查关键依赖（数据库 + Redis），用于启动时 fail-fast
     */
    public void validateCriticalDependencies() {
        log.info("=== 启动关键依赖检查 ===");
        boolean allCriticalUp = true;

        // 数据库检查
        Map<String, Object> dbHealth = checkDatabaseHealth();
        if (!"UP".equals(dbHealth.get("status"))) {
            log.error("[启动检查失败] 数据库不可用: {}", dbHealth.get("error"));
            allCriticalUp = false;
        } else {
            log.info("[启动检查通过] 数据库连接正常");
        }

        // Redis 检查
        Map<String, Object> redisHealth = checkRedisHealth();
        if (!"UP".equals(redisHealth.get("status"))) {
            log.error("[启动检查失败] Redis 不可用: {}", redisHealth.get("error"));
            allCriticalUp = false;
        } else {
            log.info("[启动检查通过] Redis 连接正常");
        }

        log.info("=== 关键依赖检查完成 ===");

        if (!allCriticalUp) {
            throw new IllegalStateException("关键依赖检查失败！数据库或 Redis 不可用，应用无法启动。");
        }
    }

    /**
     * 数据库连接健康检查
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                var meta = conn.getMetaData();
                result.put("status", "UP");
                result.put("database", meta.getDatabaseProductName());
                result.put("driver", meta.getDriverName());
                result.put("valid", true);
                return result;
            }
            result.put("status", "DOWN");
            result.put("error", "Connection validation failed");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * Redis 连接健康检查
     */
    private Map<String, Object> checkRedisHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                result.put("status", "UP");
                result.put("response", "PONG");
                return result;
            }
            result.put("status", "DOWN");
            result.put("error", "Unexpected response: " + pong);
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * 磁盘空间健康检查
     */
    private Map<String, Object> checkDiskSpaceHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            java.nio.file.FileStore store = java.nio.file.Files.getFileStore(
                    java.nio.file.Paths.get("/").toAbsolutePath());
            long totalSpace = store.getTotalSpace();
            long freeSpace = store.getUsableSpace();
            long freeSpaceMb = freeSpace / (1024 * 1024);
            long totalSpaceMb = totalSpace / (1024 * 1024);
            double percentFree = (double) freeSpace / totalSpace * 100;

            result.put("status", freeSpaceMb >= DISK_SPACE_THRESHOLD_MB ? "UP" : "DOWN");
            result.put("freeSpaceMb", freeSpaceMb);
            result.put("totalSpaceMb", totalSpaceMb);
            result.put("thresholdMb", DISK_SPACE_THRESHOLD_MB);
            result.put("percentFree", String.format("%.1f%%", percentFree));
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * 外部 LLM 提供商连通性检查
     * 检查当前默认提供商是否配置且可达
     */
    private Map<String, Object> checkLlmProviderHealth() {
        Map<String, Object> result = new HashMap<>();
        result.put("defaultProvider", aiAgentProperties.getLlm().getDefaultProvider());

        try {
            RestTemplate restTemplate = new RestTemplate();
            if (restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory f) {
                f.setConnectTimeout(5000);
                f.setReadTimeout(5000);
            }

            boolean anyProviderAvailable = false;
            Map<String, String> providerStatuses = new LinkedHashMap<>();

            // 检查 OpenAI
            String openaiApiKey = aiAgentProperties.getLlm().getOpenai().getApiKey();
            if (openaiApiKey != null && !openaiApiKey.isBlank()) {
                providerStatuses.put("openai", checkProviderEndpoint(restTemplate,
                        aiAgentProperties.getLlm().getOpenai().getBaseUrl() + "/models", "OpenAI"));
                if ("reachable".equals(providerStatuses.get("openai"))) {
                    anyProviderAvailable = true;
                }
            } else {
                providerStatuses.put("openai", "not_configured");
            }

            // 检查 Qwen
            String qwenApiKey = aiAgentProperties.getLlm().getQwen().getApiKey();
            if (qwenApiKey != null && !qwenApiKey.isBlank()) {
                providerStatuses.put("qwen", checkProviderEndpoint(restTemplate,
                        aiAgentProperties.getLlm().getQwen().getBaseUrl() + "/models", "Qwen"));
                if ("reachable".equals(providerStatuses.get("qwen"))) {
                    anyProviderAvailable = true;
                }
            } else {
                providerStatuses.put("qwen", "not_configured");
            }

            // 检查 Ollama
            providerStatuses.put("ollama", checkProviderEndpoint(restTemplate,
                    aiAgentProperties.getLlm().getOllama().getBaseUrl() + "/api/tags", "Ollama"));
            if ("reachable".equals(providerStatuses.get("ollama"))) {
                anyProviderAvailable = true;
            }

            result.put("providers", providerStatuses);
            result.put("status", anyProviderAvailable ? "UP" : "DOWN");
            if (!anyProviderAvailable) {
                result.put("warning", "No LLM provider is available. AI features will not work.");
            }
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * 检查单个 LLM 提供商端点可达性
     */
    private String checkProviderEndpoint(RestTemplate restTemplate, String url, String providerName) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 500) {
                return "reachable";
            }
            return "error_" + responseCode;
        } catch (java.net.SocketTimeoutException e) {
            return "timeout";
        } catch (java.net.ConnectException e) {
            return "unreachable";
        } catch (Exception e) {
            return "error: " + e.getClass().getSimpleName();
        }
    }

    /**
     * JWT 密钥有效性检查
     * 验证密钥已配置且可以正常签名/验证 token
     */
    private Map<String, Object> checkJwtSecretHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 尝试生成并验证一个测试 token
            String testToken = jwtUtil.generateToken(-1L, "__health_check__", -1L);
            boolean valid = jwtUtil.validateToken(testToken);

            if (valid) {
                result.put("status", "UP");
                result.put("signingWorks", true);
                result.put("validationWorks", true);
            } else {
                result.put("status", "DOWN");
                result.put("error", "Generated test token validation failed");
            }
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * 缓存命中率健康检查
     * 如果命中率低于 50% 则发出警告
     */
    private Map<String, Object> checkCacheHitRateHealth() {
        Map<String, Object> result = new HashMap<>();
        Map<String, CacheStatisticsService.CacheStats> stats = cacheStatisticsService.getStats();

        if (stats.isEmpty()) {
            result.put("status", "UP");
            result.put("message", "No cache activity recorded yet");
            result.put("hitRate", "N/A");
            return result;
        }

        long totalHits = 0;
        long totalMisses = 0;
        Map<String, String> cacheRates = new LinkedHashMap<>();

        for (Map.Entry<String, CacheStatisticsService.CacheStats> entry : stats.entrySet()) {
            CacheStatisticsService.CacheStats cacheStats = entry.getValue();
            long hits = cacheStats.getHits().get();
            long misses = cacheStats.getMisses().get();
            totalHits += hits;
            totalMisses += misses;

            long total = hits + misses;
            double rate = total > 0 ? (double) hits / total * 100 : 0;
            cacheRates.put(entry.getKey(), String.format("%.1f%%", rate));
        }

        long grandTotal = totalHits + totalMisses;
        double overallRate = grandTotal > 0 ? (double) totalHits / grandTotal * 100 : 0;

        result.put("cacheRates", cacheRates);
        result.put("overallHitRate", String.format("%.1f%%", overallRate));
        result.put("totalHits", totalHits);
        result.put("totalMisses", totalMisses);

        if (overallRate < CACHE_HIT_RATE_WARNING_THRESHOLD && grandTotal > 100) {
            result.put("status", "DEGRADED");
            result.put("warning", String.format(
                    "Cache hit rate %.1f%% is below threshold %.1f%%. Consider reviewing cache configuration.",
                    overallRate, CACHE_HIT_RATE_WARNING_THRESHOLD));
        } else {
            result.put("status", "UP");
        }

        return result;
    }
}
