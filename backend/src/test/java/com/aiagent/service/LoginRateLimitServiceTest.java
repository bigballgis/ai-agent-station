package com.aiagent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LoginRateLimitService 单元测试
 * 测试登录速率限制、失败尝试记录、重置等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("登录速率限制服务测试")
class LoginRateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginRateLimitService loginRateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("检查速率限制 - 无记录时允许")
    void testCheckRateLimit_NoRecord() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertTrue(loginRateLimitService.checkRateLimit("testuser", "192.168.1.1"));
    }

    @Test
    @DisplayName("检查速率限制 - 未达到上限时允许")
    void testCheckRateLimit_BelowLimit() {
        when(valueOperations.get(anyString())).thenReturn("3");

        assertTrue(loginRateLimitService.checkRateLimit("testuser", "192.168.1.1"));
    }

    @Test
    @DisplayName("检查速率限制 - 达到上限时拒绝")
    void testCheckRateLimit_AtLimit() {
        when(valueOperations.get(anyString())).thenReturn("5");

        assertFalse(loginRateLimitService.checkRateLimit("testuser", "192.168.1.1"));
    }

    @Test
    @DisplayName("检查速率限制 - 超过上限时拒绝")
    void testCheckRateLimit_ExceedsLimit() {
        when(valueOperations.get(anyString())).thenReturn("10");

        assertFalse(loginRateLimitService.checkRateLimit("testuser", "192.168.1.1"));
    }

    @Test
    @DisplayName("记录失败尝试 - 首次失败设置过期时间")
    void testRecordFailedAttempt_FirstAttempt() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        loginRateLimitService.recordFailedAttempt("testuser", "192.168.1.1");

        verify(valueOperations).increment(anyString());
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("记录失败尝试 - 非首次失败不重置过期时间")
    void testRecordFailedAttempt_SubsequentAttempt() {
        when(valueOperations.increment(anyString())).thenReturn(3L);

        loginRateLimitService.recordFailedAttempt("testuser", "192.168.1.1");

        verify(valueOperations).increment(anyString());
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("重置尝试计数 - 有记录时删除")
    void testResetAttempts_WithRecords() {
        String pattern = "login:attempt:testuser:*";
        when(redisTemplate.keys(pattern)).thenReturn(Collections.singleton("login:attempt:testuser:192.168.1.1"));
        when(redisTemplate.delete(anyCollection())).thenReturn(1L);

        loginRateLimitService.resetAttempts("testuser");

        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(anyCollection());
    }

    @Test
    @DisplayName("重置尝试计数 - 无记录时不报错")
    void testResetAttempts_NoRecords() {
        when(redisTemplate.keys(anyString())).thenReturn(null);

        loginRateLimitService.resetAttempts("testuser");

        verify(redisTemplate).keys(anyString());
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    @DisplayName("重置尝试计数 - 空集合不报错")
    void testResetAttempts_EmptySet() {
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        loginRateLimitService.resetAttempts("testuser");

        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    @DisplayName("完整流程 - 多次失败后触发限流")
    void testFullFlow_MultipleFailuresThenRateLimited() {
        // 前4次允许
        when(valueOperations.get(anyString())).thenReturn(null, "1", "2", "3", "4");
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L, 3L, 4L, 5L);

        for (int i = 0; i < 4; i++) {
            assertTrue(loginRateLimitService.checkRateLimit("testuser", "192.168.1.1"));
            loginRateLimitService.recordFailedAttempt("testuser", "192.168.1.1");
        }

        // 第5次应该被限流
        when(valueOperations.get(anyString())).thenReturn("5");
        assertFalse(loginRateLimitService.checkRateLimit("testuser", "192.168.1.1"));
    }

    @Test
    @DisplayName("完整流程 - 登录成功后重置计数")
    void testFullFlow_LoginSuccessResetsCount() {
        // 模拟已有失败记录
        when(valueOperations.get(anyString())).thenReturn("3");
        assertFalse(loginRateLimitService.checkRateLimit("testuser", "192.168.1.1") == false); // 3 < 5, still allowed

        // 登录成功，重置
        when(redisTemplate.keys(anyString())).thenReturn(Collections.singleton("login:attempt:testuser:192.168.1.1"));
        loginRateLimitService.resetAttempts("testuser");

        // 之后应该允许
        when(valueOperations.get(anyString())).thenReturn(null);
        assertTrue(loginRateLimitService.checkRateLimit("testuser", "192.168.1.1"));
    }
}
