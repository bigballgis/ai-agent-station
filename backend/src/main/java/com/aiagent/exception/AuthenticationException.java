package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 认证异常
 */
public class AuthenticationException extends BusinessException {

    public AuthenticationException(String message) {
        super(ResultCode.UNAUTHORIZED.getCode(), message);
    }

    public AuthenticationException() {
        super(ResultCode.UNAUTHORIZED);
    }
}
