package com.aiagent.service;

import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.exception.FileProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmResponseCacheService {

    private static final String KEY_PREFIX = "llm_cache:";

    private final StringRedisTemplate stringRedisTemplate;
    private final AiAgentProperties aiAgentProperties;

    /**
     * Get a cached LLM response.
     *
     * @param provider     the LLM provider name
     * @param systemPrompt the system prompt
     * @param userMessage  the user message
     * @return Optional containing the cached response, or empty if not found
     */
    public Optional<String> getCached(String provider, String systemPrompt, String userMessage) {
        String key = buildCacheKey(provider, systemPrompt, userMessage);
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("LLM cache HIT for provider={}, key={}", provider, key);
                return Optional.of(cached);
            }
            log.info("LLM cache MISS for provider={}, key={}", provider, key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached response for provider={}", provider, e);
            return Optional.empty();
        }
    }

    /**
     * Cache an LLM response.
     *
     * @param provider     the LLM provider name
     * @param systemPrompt the system prompt
     * @param userMessage  the user message
     * @param response     the LLM response to cache
     */
    public void cache(String provider, String systemPrompt, String userMessage, String response) {
        String key = buildCacheKey(provider, systemPrompt, userMessage);
        try {
            long ttlMinutes = aiAgentProperties.getCache().getLlmResponseTtlMinutes();
            stringRedisTemplate.opsForValue().set(key, response, Duration.ofMinutes(ttlMinutes));
            log.info("LLM response cached for provider={}, key={}, ttl={}s", provider, key, ttlMinutes * 60);
        } catch (Exception e) {
            log.error("Failed to cache response for provider={}", provider, e);
        }
    }

    /**
     * Evict a cached LLM response.
     *
     * @param provider     the LLM provider name
     * @param systemPrompt the system prompt
     * @param userMessage  the user message
     */
    public void evict(String provider, String systemPrompt, String userMessage) {
        String key = buildCacheKey(provider, systemPrompt, userMessage);
        try {
            Boolean deleted = stringRedisTemplate.delete(key);
            log.info("LLM cache evicted for provider={}, key={}, deleted={}", provider, key, deleted);
        } catch (Exception e) {
            log.error("Failed to evict cache for provider={}", provider, e);
        }
    }

    /**
     * Build a cache key from provider, system prompt, and user message using MD5 hash.
     *
     * @param provider     the LLM provider name
     * @param systemPrompt the system prompt
     * @param userMessage  the user message
     * @return the Redis cache key
     */
    private String buildCacheKey(String provider, String systemPrompt, String userMessage) {
        String raw = provider + ":" + systemPrompt + ":" + userMessage;
        String md5 = md5Hash(raw);
        return KEY_PREFIX + md5;
    }

    /**
     * Compute MD5 hash of a string.
     *
     * @param input the input string
     * @return the hexadecimal MD5 hash
     */
    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new FileProcessingException("MD5 algorithm not available", e);
        }
    }
}
