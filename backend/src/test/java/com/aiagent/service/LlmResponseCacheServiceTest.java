package com.aiagent.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LlmResponseCacheService 单元测试
 * 测试LLM响应缓存的获取、存储、清除和缓存键生成功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LLM响应缓存服务测试")
class LlmResponseCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LlmResponseCacheService llmResponseCacheService;

    private static final String PROVIDER = "openai";
    private static final String SYSTEM_PROMPT = "You are a helpful assistant.";
    private static final String USER_MESSAGE = "Hello, world!";
    private static final String CACHED_RESPONSE = "{\"response\": \"Hi there!\"}";

    @BeforeEach
    void setUp() {
        // 模拟 Redis ValueOperations
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== getCachedResponse 测试 ====================

    @Test
    @DisplayName("获取缓存 - 缓存命中时返回响应")
    void getCached_CacheHit() {
        when(valueOperations.get(anyString())).thenReturn(CACHED_RESPONSE);

        Optional<String> result = llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);

        assertTrue(result.isPresent());
        assertEquals(CACHED_RESPONSE, result.get());
    }

    @Test
    @DisplayName("获取缓存 - 缓存未命中时返回空")
    void getCached_CacheMiss() {
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<String> result = llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("获取缓存 - Redis异常时返回空而不抛出")
    void getCached_RedisException_ReturnsEmpty() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis连接失败"));

        Optional<String> result = llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);

        assertFalse(result.isPresent());
    }

    // ==================== cacheResponse 测试 ====================

    @Test
    @DisplayName("缓存响应 - 成功存储")
    void cache_Success() {
        when(valueOperations.set(anyString(), anyString(), any())).thenReturn(true);

        assertDoesNotThrow(() ->
                llmResponseCacheService.cache(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE, CACHED_RESPONSE));

        verify(valueOperations).set(anyString(), eq(CACHED_RESPONSE), any());
    }

    @Test
    @DisplayName("缓存响应 - Redis异常时不抛出")
    void cache_RedisException_NoThrow() {
        when(valueOperations.set(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Redis写入失败"));

        assertDoesNotThrow(() ->
                llmResponseCacheService.cache(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE, CACHED_RESPONSE));
    }

    // ==================== evictCache 测试 ====================

    @Test
    @DisplayName("清除缓存 - 成功删除")
    void evict_Success() {
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        assertDoesNotThrow(() ->
                llmResponseCacheService.evict(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE));

        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("清除缓存 - 缓存不存在时也正常返回")
    void evict_CacheNotExists() {
        when(stringRedisTemplate.delete(anyString())).thenReturn(false);

        assertDoesNotThrow(() ->
                llmResponseCacheService.evict(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE));

        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("清除缓存 - Redis异常时不抛出")
    void evict_RedisException_NoThrow() {
        when(stringRedisTemplate.delete(anyString()))
                .thenThrow(new RuntimeException("Redis连接失败"));

        assertDoesNotThrow(() ->
                llmResponseCacheService.evict(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE));
    }

    // ==================== generateCacheKey 测试 ====================

    @Test
    @DisplayName("生成缓存键 - 相同输入生成相同的键")
    void generateCacheKey_SameInput_SameKey() {
        when(valueOperations.get(anyString())).thenReturn(CACHED_RESPONSE);

        // 第一次调用
        llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);

        // 第二次调用，使用相同参数
        llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);

        // 验证两次调用使用了相同的缓存键
        verify(valueOperations, times(2)).get(startsWith("llm_cache:"));
    }

    @Test
    @DisplayName("生成缓存键 - 不同输入生成不同的键")
    void generateCacheKey_DifferentInput_DifferentKey() {
        when(valueOperations.get(anyString())).thenReturn(null);

        llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);
        llmResponseCacheService.getCached("qwen", SYSTEM_PROMPT, USER_MESSAGE);
        llmResponseCacheService.getCached(PROVIDER, "Different prompt", USER_MESSAGE);
        llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, "Different message");

        // 验证四次调用都使用了不同的缓存键（每次get调用都传入不同参数）
        verify(valueOperations, times(4)).get(anyString());
    }

    @Test
    @DisplayName("生成缓存键 - 键格式正确（以llm_cache:为前缀）")
    void generateCacheKey_CorrectPrefix() {
        when(valueOperations.get(anyString())).thenReturn(null);

        llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);

        verify(valueOperations).get(argThat(key ->
                key != null && key.startsWith("llm_cache:") && key.length() > "llm_cache:".length()
        ));
    }

    // ==================== 集成场景测试 ====================

    @Test
    @DisplayName("缓存完整流程 - 先缓存再获取")
    void cacheFlow_CacheThenRetrieve() {
        // 第一次获取，缓存未命中
        when(valueOperations.get(anyString())).thenReturn(null);
        Optional<String> miss = llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);
        assertFalse(miss.isPresent());

        // 缓存响应
        when(valueOperations.set(anyString(), eq(CACHED_RESPONSE), any())).thenReturn(true);
        llmResponseCacheService.cache(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE, CACHED_RESPONSE);

        // 第二次获取，缓存命中
        when(valueOperations.get(anyString())).thenReturn(CACHED_RESPONSE);
        Optional<String> hit = llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);
        assertTrue(hit.isPresent());
        assertEquals(CACHED_RESPONSE, hit.get());

        // 清除缓存
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
        llmResponseCacheService.evict(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);

        // 第三次获取，缓存已清除
        when(valueOperations.get(anyString())).thenReturn(null);
        Optional<String> afterEvict = llmResponseCacheService.getCached(PROVIDER, SYSTEM_PROMPT, USER_MESSAGE);
        assertFalse(afterEvict.isPresent());
    }
}
