package com.aiagent.exception;

import com.aiagent.common.ResultCode;

public class BusinessException extends RuntimeException {
    private final Integer code;

    public Integer getCode() {
        return code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_SERVER_ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
