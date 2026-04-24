package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 文件处理异常 (422)
 */
public class FileProcessingException extends BusinessException {

    public FileProcessingException(String message) {
        super(ResultCode.FILE_PROCESSING_FAILED.getCode(), ResultCode.FILE_PROCESSING_FAILED.getMessageCode(), message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(ResultCode.FILE_PROCESSING_FAILED.getCode(), ResultCode.FILE_PROCESSING_FAILED.getMessageCode(), message);
        initCause(cause);
    }

    public FileProcessingException() {
        super(ResultCode.FILE_PROCESSING_FAILED);
    }
}
