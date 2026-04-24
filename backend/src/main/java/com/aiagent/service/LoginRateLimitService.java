package com.aiagent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录速率限制服务
 * 使用 Redis INCR + EXPIRE 实现滑动窗口限流
 * 默认: 每分钟最多5次失败尝试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRateLimitService {

    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
    private static final String IP_GLOBAL_PREFIX = "login:ip:global:";

    @Value("${ai-agent.rate-limit.max-attempts-per-minute:5}")
    private int maxAttemptsPerMinute;

    @Value("${ai-agent.rate-limit.max-ip-global-per-minute:10}")
    private int maxIpGlobalAttemptsPerMinute;

    @Value("${ai-agent.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Value("${ai-agent.rate-limit.base-backoff-seconds:2}")
    private long baseBackoffSeconds;

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查是否超过速率限制
     *
     * @param username 用户名
     * @param ip       客户端IP
     * @return null 表示允许尝试, 非null字符串表示限流提示信息
     */
    public String checkRateLimit(String username, String ip) {
        // 1. 基于IP的全局限制检查（每IP每分钟最多10次）
        String ipGlobalKey = IP_GLOBAL_PREFIX + ip;
        String ipCountStr = redisTemplate.opsForValue().get(ipGlobalKey);
        if (ipCountStr != null) {
            int ipCount = Integer.parseInt(ipCountStr);
            if (ipCount >= maxIpGlobalAttemptsPerMinute) {
                log.warn("IP全局登录速率限制触发: ip={}, attempts={}", ip, ipCount);
                return "登录尝试次数过多，请稍后再试";
            }
        }

        // 2. 基于用户名+IP的限制检查
        String key = buildKey(username, ip);
        String countStr = redisTemplate.opsForValue().get(key);

        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            if (count >= maxAttemptsPerMinute) {
                // 指数退避：基于失败次数计算建议等待时间
                long backoffSeconds = baseBackoffSeconds * (long) Math.pow(2, Math.min(count - maxAttemptsPerMinute, 4));
                String message = String.format("登录尝试次数过多，请%d秒后再试", backoffSeconds);
                log.warn("登录速率限制触发: username={}, ip={}, attempts={}, backoff={}s", username, ip, count, backoffSeconds);
                return message;
            }
        }

        return null;
    }

    /**
     * 检查是否超过速率限制（兼容旧接口）
     *
     * @param username 用户名
     * @param ip       客户端IP
     * @return true 表示允许尝试, false 表示已被限流
     */
    public boolean checkRateLimitBool(String username, String ip) {
        return checkRateLimit(username, ip) == null;
    }

    /**
     * 记录一次失败尝试
     *
     * @param username 用户名
     * @param ip       客户端IP
     */
    public void recordFailedAttempt(String username, String ip) {
        String key = buildKey(username, ip);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次失败时设置过期时间
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        // 同时增加IP全局计数
        String ipGlobalKey = IP_GLOBAL_PREFIX + ip;
        Long ipCount = redisTemplate.opsForValue().increment(ipGlobalKey);
        if (ipCount != null && ipCount == 1) {
            redisTemplate.expire(ipGlobalKey, windowSeconds, TimeUnit.SECONDS);
        }

        log.info("记录登录失败尝试: username={}, ip={}, count={}, ipGlobalCount={}", username, ip, count, ipCount);
    }

    /**
     * 登录成功后重置尝试计数
     *
     * @param username 用户名
     */
    public void resetAttempts(String username) {
        // 按用户名模糊删除所有相关 key (username:*)
        String pattern = LOGIN_ATTEMPT_PREFIX + username + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("重置登录尝试计数: username={}, deletedKeys={}", username, keys.size());
        }
    }

    private String buildKey(String username, String ip) {
        return LOGIN_ATTEMPT_PREFIX + username + ":" + ip;
    }
}
