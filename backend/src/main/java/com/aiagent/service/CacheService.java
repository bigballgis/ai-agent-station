package com.aiagent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    // 缓存常量
    public static final String DICT_CACHE_PREFIX = "dict:";
    public static final String USER_CACHE_PREFIX = "user:";
    public static final String AGENT_CACHE_PREFIX = "agent:";
    public static final String QUOTA_CACHE_PREFIX = "quota:";

    @Value("${ai-agent.cache.default-ttl-minutes:30}")
    private long defaultTtlMinutes;

    @Value("${ai-agent.cache.dict-ttl-hours:2}")
    private long dictTtlHours;

    /**
     * 设置缓存
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 按前缀批量删除缓存
     * 使用 SCAN 替代 KEYS，避免在生产环境阻塞 Redis
     */
    public void deleteByPrefix(String prefix) {
        List<String> keysToDelete = new ArrayList<>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
            }
        }
        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
            log.info("按前缀清除缓存: prefix={}, count={}", prefix, keysToDelete.size());
        }
    }

    /**
     * 判断缓存是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ==================== 字典缓存辅助方法 ====================

    /**
     * 缓存字典项
     */
    public void cacheDictItems(String dictType, String json) {
        set(DICT_CACHE_PREFIX + dictType, json, dictTtlHours, TimeUnit.HOURS);
    }

    /**
     * 获取缓存的字典项
     */
    public String getCachedDictItems(String dictType) {
        return get(DICT_CACHE_PREFIX + dictType);
    }

    /**
     * 清除指定字典类型的缓存
     */
    public void evictDictCache(String dictType) {
        delete(DICT_CACHE_PREFIX + dictType);
    }

    /**
     * 清除所有字典缓存
     */
    public void evictAllDictCache() {
        deleteByPrefix(DICT_CACHE_PREFIX);
    }

    // ==================== 用户缓存辅助方法 ====================

    /**
     * 缓存用户信息
     */
    public void cacheUserInfo(Long userId, String json) {
        set(USER_CACHE_PREFIX + userId, json, defaultTtlMinutes, TimeUnit.MINUTES);
    }

    /**
     * 获取缓存的用户信息
     */
    public String getCachedUserInfo(Long userId) {
        return get(USER_CACHE_PREFIX + userId);
    }

    /**
     * 清除用户缓存
     */
    public void evictUserCache(Long userId) {
        delete(USER_CACHE_PREFIX + userId);
    }

    // ==================== Agent缓存辅助方法 ====================

    /**
     * 缓存Agent信息
     */
    public void cacheAgentInfo(Long agentId, String json) {
        set(AGENT_CACHE_PREFIX + agentId, json, defaultTtlMinutes, TimeUnit.MINUTES);
    }

    /**
     * 获取缓存的Agent信息
     */
    public String getCachedAgentInfo(Long agentId) {
        return get(AGENT_CACHE_PREFIX + agentId);
    }

    /**
     * 清除Agent缓存
     */
    public void evictAgentCache(Long agentId) {
        delete(AGENT_CACHE_PREFIX + agentId);
    }

    // ==================== 配额缓存辅助方法 ====================

    /**
     * 缓存配额信息
     */
    public void cacheQuota(Long tenantId, String json) {
        set(QUOTA_CACHE_PREFIX + tenantId, json, defaultTtlMinutes, TimeUnit.MINUTES);
    }

    /**
     * 获取缓存的配额信息
     */
    public String getCachedQuota(Long tenantId) {
        return get(QUOTA_CACHE_PREFIX + tenantId);
    }

    /**
     * 清除配额缓存
     */
    public void evictQuotaCache(Long tenantId) {
        delete(QUOTA_CACHE_PREFIX + tenantId);
    }
}
