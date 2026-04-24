package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessageCode(), message);
    }

    public ResourceNotFoundException() {
        super(ResultCode.NOT_FOUND);
    }
}
