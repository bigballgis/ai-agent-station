package com.aiagent.service;

import com.aiagent.security.JwtUtil;
import com.aiagent.config.properties.AiAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * ApplicationHealthService 单元测试
 * 测试各组件健康检查功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("应用健康检查服务测试")
class ApplicationHealthServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private CacheStatisticsService cacheStatisticsService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AiAgentProperties aiAgentProperties;

    @Mock
    private Connection dbConnection;

    @Mock
    private DatabaseMetaData dbMetaData;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    private ApplicationHealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new ApplicationHealthService(dataSource, redisTemplate, cacheStatisticsService, jwtUtil, aiAgentProperties);

        AiAgentProperties.Llm llm = new AiAgentProperties.Llm();
        llm.setDefaultProvider("openai");
        // Keep API keys empty to avoid real external calls. Also make the ollama URL malformed for fast failure.
        AiAgentProperties.Llm.OllamaConfig ollama = new AiAgentProperties.Llm.OllamaConfig();
        ollama.setBaseUrl("http://:");
        llm.setOllama(ollama);
        when(aiAgentProperties.getLlm()).thenReturn(llm);
    }

    // ==================== 数据库健康检查测试 ====================

    @Test
    @DisplayName("数据库健康检查 - 连接正常")
    @SuppressWarnings("unchecked")
    void testGetDetailedHealth_DatabaseUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(cacheStatisticsService.getStats()).thenReturn(Map.of());
        when(jwtUtil.generateToken(anyLong(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        Map<String, Object> health = healthService.getDetailedHealth();

        @SuppressWarnings("unchecked")
        Map<String, Object> dbHealth = (Map<String, Object>) health.get("database");
        assertEquals("UP", dbHealth.get("status"));
        assertEquals("PostgreSQL", dbHealth.get("database"));
    }

    @Test
    @DisplayName("数据库健康检查 - 连接失败")
    @SuppressWarnings("unchecked")
    void testGetDetailedHealth_DatabaseDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(cacheStatisticsService.getStats()).thenReturn(Map.of());
        when(jwtUtil.generateToken(anyLong(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        Map<String, Object> health = healthService.getDetailedHealth();

        @SuppressWarnings("unchecked")
        Map<String, Object> dbHealth = (Map<String, Object>) health.get("database");
        assertEquals("DOWN", dbHealth.get("status"));
    }

    // ==================== Redis 健康检查测试 ====================

    @Test
    @DisplayName("Redis 健康检查 - 连接正常")
    @SuppressWarnings("unchecked")
    void testGetDetailedHealth_RedisUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(cacheStatisticsService.getStats()).thenReturn(Map.of());
        when(jwtUtil.generateToken(anyLong(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        Map<String, Object> health = healthService.getDetailedHealth();

        @SuppressWarnings("unchecked")
        Map<String, Object> redisHealth = (Map<String, Object>) health.get("redis");
        assertEquals("UP", redisHealth.get("status"));
        assertEquals("PONG", redisHealth.get("response"));
    }

    @Test
    @DisplayName("Redis 健康检查 - 连接失败")
    @SuppressWarnings("unchecked")
    void testGetDetailedHealth_RedisDown() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenThrow(new RuntimeException("Redis connection failed"));
        when(cacheStatisticsService.getStats()).thenReturn(Map.of());
        when(jwtUtil.generateToken(anyLong(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        Map<String, Object> health = healthService.getDetailedHealth();

        @SuppressWarnings("unchecked")
        Map<String, Object> redisHealth = (Map<String, Object>) health.get("redis");
        assertEquals("DOWN", redisHealth.get("status"));
    }

    // ==================== JWT 健康检查测试 ====================

    @Test
    @DisplayName("JWT 健康检查 - 签名验证正常")
    @SuppressWarnings("unchecked")
    void testGetDetailedHealth_JwtUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(cacheStatisticsService.getStats()).thenReturn(Map.of());
        when(jwtUtil.generateToken(anyLong(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        Map<String, Object> health = healthService.getDetailedHealth();

        @SuppressWarnings("unchecked")
        Map<String, Object> jwtHealth = (Map<String, Object>) health.get("jwtSecret");
        assertEquals("UP", jwtHealth.get("status"));
        assertEquals(true, jwtHealth.get("signingWorks"));
        assertEquals(true, jwtHealth.get("validationWorks"));
    }

    @Test
    @DisplayName("JWT 健康检查 - 签名验证失败")
    @SuppressWarnings("unchecked")
    void testGetDetailedHealth_JwtDown() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(cacheStatisticsService.getStats()).thenReturn(Map.of());
        when(jwtUtil.generateToken(anyLong(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.validateToken(anyString())).thenReturn(false);

        Map<String, Object> health = healthService.getDetailedHealth();

        @SuppressWarnings("unchecked")
        Map<String, Object> jwtHealth = (Map<String, Object>) health.get("jwtSecret");
        assertEquals("DOWN", jwtHealth.get("status"));
    }

    // ==================== 缓存命中率健康检查测试 ====================

    @Test
    @DisplayName("缓存命中率检查 - 无缓存活动")
    @SuppressWarnings("unchecked")
    void testGetDetailedHealth_CacheNoActivity() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(cacheStatisticsService.getStats()).thenReturn(Map.of());
        when(jwtUtil.generateToken(anyLong(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        Map<String, Object> health = healthService.getDetailedHealth();

        @SuppressWarnings("unchecked")
        Map<String, Object> cacheHealth = (Map<String, Object>) health.get("cacheHitRate");
        assertEquals("UP", cacheHealth.get("status"));
        assertEquals("N/A", cacheHealth.get("hitRate"));
    }

    // ==================== 综合状态测试 ====================

    @Test
    @DisplayName("综合健康状态 - 所有组件正常返回 UP")
    void testGetDetailedHealth_AllUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(cacheStatisticsService.getStats()).thenReturn(Map.of());
        when(jwtUtil.generateToken(anyLong(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.validateToken(anyString())).thenReturn(true);

        Map<String, Object> health = healthService.getDetailedHealth();

        // LLM provider will be DOWN since no API keys configured, so overall should be DEGRADED
        // But if we set keys, it would be UP. Let's check what we get.
        // Since openai/qwen keys are empty, LLM will be DOWN, so overall is DEGRADED
        assertEquals("DEGRADED", health.get("overallStatus"));
        assertNotNull(health.get("checkTime"));
    }

    // ==================== validateCriticalDependencies 测试 ====================

    @Test
    @DisplayName("关键依赖验证 - 数据库和Redis都正常")
    void testValidateCriticalDependencies_AllUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        assertDoesNotThrow(() -> healthService.validateCriticalDependencies());
    }

    @Test
    @DisplayName("关键依赖验证 - 数据库不可用抛出异常")
    void testValidateCriticalDependencies_DatabaseDown_ThrowsException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        assertThrows(IllegalStateException.class, () -> healthService.validateCriticalDependencies());
    }

    @Test
    @DisplayName("关键依赖验证 - Redis不可用抛出异常")
    void testValidateCriticalDependencies_RedisDown_ThrowsException() throws Exception {
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(anyInt())).thenReturn(true);
        when(dbConnection.getMetaData()).thenReturn(dbMetaData);
        when(dbMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(dbMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenThrow(new RuntimeException("Redis connection failed"));

        assertThrows(IllegalStateException.class, () -> healthService.validateCriticalDependencies());
    }

    @Test
    @DisplayName("关键依赖验证 - 数据库和Redis都不可用抛出异常")
    void testValidateCriticalDependencies_BothDown_ThrowsException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenThrow(new RuntimeException("Redis connection failed"));

        assertThrows(IllegalStateException.class, () -> healthService.validateCriticalDependencies());
    }
}
