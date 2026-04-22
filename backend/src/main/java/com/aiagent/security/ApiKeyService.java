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

    public boolean validateApiKey(String apiKey) {
        String key = API_KEY_PREFIX + apiKey;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    public Long getTenantIdByApiKey(String apiKey) {
        String key = API_KEY_PREFIX + apiKey;
        Object tenantId = redisTemplate.opsForValue().get(key);
        return tenantId != null ? Long.valueOf(tenantId.toString()) : null;
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
