package com.aiagent.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存统计服务
 *
 * 跟踪各缓存区域的命中/未命中次数，定期输出统计日志，
 * 并提供 actuator 端点可用的统计数据。
 */
@Slf4j
@Service
public class CacheStatisticsService {

    private final Map<String, CacheStats> statsMap = new ConcurrentHashMap<>();

    /**
     * 记录缓存命中
     */
    public void recordHit(String cacheName) {
        statsMap.computeIfAbsent(cacheName, k -> new CacheStats()).hits.incrementAndGet();
    }

    /**
     * 记录缓存未命中
     */
    public void recordMiss(String cacheName) {
        statsMap.computeIfAbsent(cacheName, k -> new CacheStats()).misses.incrementAndGet();
    }

    /**
     * 获取所有缓存统计信息
     */
    public Map<String, CacheStats> getStats() {
        return statsMap;
    }

    /**
     * 获取指定缓存区域的统计信息
     */
    public CacheStats getStats(String cacheName) {
        return statsMap.getOrDefault(cacheName, new CacheStats());
    }

    /**
     * 每5分钟输出一次缓存命中率统计日志
     */
    @Scheduled(fixedRate = 300000)
    public void logCacheStatistics() {
        if (statsMap.isEmpty()) {
            return;
        }
        log.info("=== 缓存命中率统计 ===");
        for (Map.Entry<String, CacheStats> entry : statsMap.entrySet()) {
            CacheStats stats = entry.getValue();
            long total = stats.hits.get() + stats.misses.get();
            double hitRate = total > 0 ? (double) stats.hits.get() / total * 100 : 0;
            log.info("缓存区域: {} | 命中: {} | 未命中: {} | 命中率: {:.2f}%",
                    entry.getKey(), stats.hits.get(), stats.misses.get(), hitRate);
        }
        log.info("=======================");
    }

    @Data
    public static class CacheStats {
        private AtomicLong hits = new AtomicLong(0);
        private AtomicLong misses = new AtomicLong(0);
    }
}
