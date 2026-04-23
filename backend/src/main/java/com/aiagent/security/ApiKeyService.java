package com.aiagent.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String API_KEY_PREFIX = "api_key:";

    /**
     * 验证API Key是否有效。
     *
     * @param apiKey API Key字符串
     * @return 如果API Key有效返回true，否则返回false
     */
    public boolean validateApiKey(String apiKey) {
        String key = API_KEY_PREFIX + apiKey;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 根据API Key获取关联的租户ID。
     *
     * @param apiKey API Key字符串
     * @return 关联的租户ID，如果不存在返回null
     */
    public Long getTenantIdByApiKey(String apiKey) {
        String key = API_KEY_PREFIX + apiKey;
        Object tenantId = redisTemplate.opsForValue().get(key);
        return tenantId != null ? Long.valueOf(tenantId.toString()) : null;
    }

    /**
     * 根据API Key获取关联的用户ID。
     *
     * 注意：当前Redis中存储的API Key仅关联了tenantId，没有直接存储userId。
     * 这是已知的限制。如果需要用户级别的隔离，需要：
     * 1. 在API Key创建时同时存储userId到Redis（例如使用Hash结构存储 {tenantId, userId}）
     * 2. 或者通过tenantId查询关联的用户表获取userId
     * 当前实现返回null，调用方应根据业务需求决定是否需要扩展。
     *
     * @param apiKey API Key字符串
     * @return 关联的用户ID，当前实现始终返回null（已知限制）
     */
    public Long getUserIdByApiKey(String apiKey) {
        // 已知限制：当前API Key在Redis中仅存储tenantId，未关联userId。
        // 如需用户级别隔离，需在saveApiKey时同时存储userId，
        // 或通过tenantId反查用户关联关系。
        log.debug("getUserIdByApiKey called - returning null (userId not stored in API key cache)");
        return null;
    }

    public void saveApiKey(String apiKey, Long tenantId, long ttlSeconds) {
        String key = API_KEY_PREFIX + apiKey;
        redisTemplate.opsForValue().set(key, tenantId, ttlSeconds, TimeUnit.SECONDS);
    }

    public void revokeApiKey(String apiKey) {
        String key = API_KEY_PREFIX + apiKey;
        redisTemplate.delete(key);
    }
}
