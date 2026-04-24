package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 速率限制超出异常
 *
 * 与 RateLimitException 的区别:
 * - RateLimitException: 通用限流异常（用于登录限流等场景）
 * - RateLimitExceededException: API 速率限制超出（携带详细的限流元数据）
 *
 * 包含限流相关的上下文信息（限制值、剩余次数、重置时间等），
 * 供 GlobalExceptionHandler 写入响应头。
 */
public class RateLimitExceededException extends BusinessException {

    /** 限流窗口内允许的最大请求数 */
    private final int limit;

    /** 当前窗口内剩余的请求数 */
    private final int remaining;

    /** 限流窗口重置时间（Unix 时间戳，秒） */
    private final long resetTime;

    /** 限流窗口大小（秒） */
    private final int windowSeconds;

    public RateLimitExceededException(String message, int limit, int remaining, long resetTime, int windowSeconds) {
        super(ResultCode.TOO_MANY_REQUESTS.getCode(), ResultCode.TOO_MANY_REQUESTS.getMessageCode(), message);
        this.limit = limit;
        this.remaining = remaining;
        this.resetTime = resetTime;
        this.windowSeconds = windowSeconds;
    }

    public RateLimitExceededException(int limit, int remaining, long resetTime, int windowSeconds) {
        this("请求过于频繁，请稍后再试", limit, remaining, resetTime, windowSeconds);
    }

    public int getLimit() {
        return limit;
    }

    public int getRemaining() {
        return remaining;
    }

    public long getResetTime() {
        return resetTime;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }
}
