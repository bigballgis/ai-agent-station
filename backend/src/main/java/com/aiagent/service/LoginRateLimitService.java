package com.aiagent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final int MAX_ATTEMPTS_PER_MINUTE = 5;
    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查是否超过速率限制
     *
     * @param username 用户名
     * @param ip       客户端IP
     * @return true 表示允许尝试, false 表示已被限流
     */
    public boolean checkRateLimit(String username, String ip) {
        String key = buildKey(username, ip);
        String countStr = redisTemplate.opsForValue().get(key);

        if (countStr == null) {
            return true;
        }

        int count = Integer.parseInt(countStr);
        if (count >= MAX_ATTEMPTS_PER_MINUTE) {
            log.warn("登录速率限制触发: username={}, ip={}, attempts={}", username, ip, count);
            return false;
        }

        return true;
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
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        log.info("记录登录失败尝试: username={}, ip={}, count={}", username, ip, count);
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
