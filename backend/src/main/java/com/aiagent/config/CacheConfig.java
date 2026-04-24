package com.aiagent.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.redis.RedisCacheConfiguration;
import org.springframework.cache.redis.RedisCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Redis 缓存配置
 *
 * 性能优化:
 * - 使用 Redis 作为缓存中心，减少数据库查询
 * - 不同缓存区域设置不同的 TTL，平衡数据新鲜度和性能
 * - 使用 JSON 序列化，支持跨服务缓存读取
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("agents", config.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("tools", config.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("permissions", config.entryTtl(Duration.ofHours(1)))
            .build();
    }
}
