package com.aiagent.config;

import com.aiagent.service.CacheStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.redis.RedisCacheConfiguration;
import org.springframework.cache.redis.RedisCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Redis 缓存配置
 *
 * 性能优化:
 * - 使用 Redis 作为缓存中心，减少数据库查询
 * - 不同缓存区域设置不同的 TTL，平衡数据新鲜度和性能
 * - 使用 JSON 序列化，支持跨服务缓存读取
 * - 支持缓存命中率统计，便于性能监控和调优
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final CacheStatisticsService cacheStatisticsService;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(factory);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(cacheWriter)
            .cacheDefaults(defaultConfig)
            // Agent 相关缓存（10分钟）
            .withCacheConfiguration("agents", defaultConfig.entryTtl(Duration.ofMinutes(10)))
            // 工具缓存（30分钟）
            .withCacheConfiguration("tools", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            // 权限矩阵缓存（1小时，极少变更）
            .withCacheConfiguration("permissions", defaultConfig.entryTtl(Duration.ofHours(1)))
            // Agent 模板列表缓存（5分钟，变更较少）
            .withCacheConfiguration("templates", defaultConfig.entryTtl(Duration.ofMinutes(5)))
            // 字典/类型列表缓存（30分钟）
            .withCacheConfiguration("dictItemsMap", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            // 字典类型列表缓存（30分钟）
            .withCacheConfiguration("dictTypes", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            // 租户配置缓存（10分钟）
            .withCacheConfiguration("tenantConfig", defaultConfig.entryTtl(Duration.ofMinutes(10)))
            // 权限列表缓存（1小时）
            .withCacheConfiguration("permissionList", defaultConfig.entryTtl(Duration.ofHours(1)))
            .build();
    }

    /**
     * 自定义 Key 生成器：支持基于租户隔离的缓存 key
     * key 格式: tenantId:methodName:argsHash
     */
    @Bean("tenantAwareKeyGenerator")
    public KeyGenerator tenantAwareKeyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                // 加入租户 ID 隔离
                Long tenantId = com.aiagent.tenant.TenantContextHolder.getTenantId();
                if (tenantId != null) {
                    sb.append("t:").append(tenantId).append(":");
                }
                sb.append(method.getName());
                for (Object param : params) {
                    sb.append(":").append(param != null ? param.hashCode() : "null");
                }
                return sb.toString();
            }
        };
    }
}
