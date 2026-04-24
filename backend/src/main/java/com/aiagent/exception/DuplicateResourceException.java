package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 资源重复异常 (409)
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), ResultCode.RESOURCE_ALREADY_EXISTS.getMessageCode(), message);
    }

    public DuplicateResourceException() {
        super(ResultCode.RESOURCE_ALREADY_EXISTS);
    }
}
