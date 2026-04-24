package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 参数校验异常 (400)
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ResultCode.VALIDATION_FAILED.getCode(), ResultCode.VALIDATION_FAILED.getMessageCode(), message);
    }

    public ValidationException() {
        super(ResultCode.VALIDATION_FAILED);
    }
}
