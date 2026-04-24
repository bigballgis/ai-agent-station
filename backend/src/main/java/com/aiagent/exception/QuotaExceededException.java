package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 配额超限异常 (429)
 */
public class QuotaExceededException extends BusinessException {

    public QuotaExceededException(String message) {
        super(ResultCode.TOO_MANY_REQUESTS.getCode(), ResultCode.TOO_MANY_REQUESTS.getMessageCode(), message);
    }

    public QuotaExceededException() {
        super(ResultCode.TOO_MANY_REQUESTS);
    }
}
