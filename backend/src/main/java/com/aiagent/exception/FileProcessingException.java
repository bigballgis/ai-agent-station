package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 文件处理异常 (422)
 */
public class FileProcessingException extends BusinessException {

    public FileProcessingException(String message) {
        super(422, message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(422, message);
        initCause(cause);
    }
}
