package com.aiagent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.KeyBoundCursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CacheService 单元测试
 * 测试缓存设置、获取、删除等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("缓存服务测试")
class CacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("设置和获取缓存 - 成功")
    void testSetAndGet() {
        cacheService.set("test:key", "test_value", 30, TimeUnit.MINUTES);

        verify(valueOperations).set("test:key", "test_value", 30, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("获取缓存 - 返回值")
    void testGet() {
        when(valueOperations.get("test:key")).thenReturn("cached_value");

        String result = cacheService.get("test:key");

        assertEquals("cached_value", result);
        verify(valueOperations).get("test:key");
    }

    @Test
    @DisplayName("获取缓存 - 不存在返回null")
    void testGet_NotFound() {
        when(valueOperations.get("nonexistent:key")).thenReturn(null);

        String result = cacheService.get("nonexistent:key");

        assertNull(result);
    }

    @Test
    @DisplayName("删除缓存 - 成功")
    void testDelete() {
        when(redisTemplate.delete("test:key")).thenReturn(true);

        cacheService.delete("test:key");

        verify(redisTemplate).delete("test:key");
    }

    @Test
    @DisplayName("按前缀批量删除缓存 - 成功")
    @SuppressWarnings("unchecked")
    void testDeleteByPrefix() {
        Set<String> keys = new HashSet<>(Arrays.asList("dict:user_status", "dict:order_status"));
        when(redisTemplate.keys("dict:*")).thenReturn(keys);
        when(redisTemplate.delete(anyCollection())).thenReturn(2L);

        cacheService.deleteByPrefix("dict:");

        verify(redisTemplate).keys("dict:*");
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("按前缀批量删除缓存 - 无匹配key")
    @SuppressWarnings("unchecked")
    void testDeleteByPrefix_NoKeys() {
        when(redisTemplate.keys("nonexistent:*")).thenReturn(null);

        cacheService.deleteByPrefix("nonexistent:");

        verify(redisTemplate).keys("nonexistent:*");
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    @DisplayName("判断缓存是否存在 - 存在")
    void testExists_True() {
        when(redisTemplate.hasKey("test:key")).thenReturn(true);

        assertTrue(cacheService.exists("test:key"));
    }

    @Test
    @DisplayName("判断缓存是否存在 - 不存在")
    void testExists_False() {
        when(redisTemplate.hasKey("test:key")).thenReturn(false);

        assertFalse(cacheService.exists("test:key"));
    }

    @Test
    @DisplayName("缓存字典项 - 使用正确前缀")
    void testCacheDictItems() {
        cacheService.cacheDictItems("user_status", "{\"items\":[]}");

        verify(valueOperations).set(
                eq("dict:user_status"),
                eq("{\"items\":[]}"),
                eq(2L),
                eq(TimeUnit.HOURS)
        );
    }

    @Test
    @DisplayName("获取缓存的字典项 - 使用正确前缀")
    void testGetCachedDictItems() {
        when(valueOperations.get("dict:user_status")).thenReturn("{\"items\":[]}");

        String result = cacheService.getCachedDictItems("user_status");

        assertEquals("{\"items\":[]}", result);
    }

    @Test
    @DisplayName("清除字典缓存 - 使用正确前缀")
    void testEvictDictCache() {
        cacheService.evictDictCache("user_status");

        verify(redisTemplate).delete("dict:user_status");
    }

    @Test
    @DisplayName("缓存用户信息 - 使用正确前缀和TTL")
    void testCacheUserInfo() {
        cacheService.cacheUserInfo(1L, "{\"id\":1}");

        verify(valueOperations).set(
                eq("user:1"),
                eq("{\"id\":1}"),
                eq(30L),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    @DisplayName("缓存Agent信息 - 使用正确前缀")
    void testCacheAgentInfo() {
        cacheService.cacheAgentInfo(1L, "{\"id\":1}");

        verify(valueOperations).set(
                eq("agent:1"),
                eq("{\"id\":1}"),
                eq(30L),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    @DisplayName("缓存配额信息 - 使用正确前缀")
    void testCacheQuota() {
        cacheService.cacheQuota(1L, "{\"quota\":{}}");

        verify(valueOperations).set(
                eq("quota:1"),
                eq("{\"quota\":{}}"),
                eq(30L),
                eq(TimeUnit.MINUTES)
        );
    }
}
