package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 请求频率限制异常
 */
public class RateLimitException extends BusinessException {

    public RateLimitException(String message) {
        super(ResultCode.TOO_MANY_REQUESTS.getCode(), ResultCode.TOO_MANY_REQUESTS.getMessageCode(), message);
    }

    public RateLimitException() {
        super(ResultCode.TOO_MANY_REQUESTS);
    }
}
