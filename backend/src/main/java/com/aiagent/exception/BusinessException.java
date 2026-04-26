package com.aiagent.exception;

import com.aiagent.common.ResultCode;

public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String messageCode;

    public Integer getCode() {
        return code;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_SERVER_ERROR.getCode();
        this.messageCode = null;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.INTERNAL_SERVER_ERROR.getCode();
        this.messageCode = null;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.messageCode = null;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.messageCode = null;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.messageCode = resultCode.getMessageCode();
    }

    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
        this.messageCode = resultCode.getMessageCode();
    }

    /**
     * Constructor with message code for i18n support.
     *
     * @param messageCode   the i18n message code (e.g. "error.agent.not_found")
     * @param defaultMessage the fallback default message
     * @param args           optional format arguments for the message
     */
    public BusinessException(String messageCode, String defaultMessage, Object... args) {
        super(formatMessage(defaultMessage, args));
        this.code = ResultCode.INTERNAL_SERVER_ERROR.getCode();
        this.messageCode = messageCode;
    }

    /**
     * Constructor with message code and explicit HTTP status code for i18n support.
     *
     * @param code          the HTTP status / business error code
     * @param messageCode   the i18n message code
     * @param defaultMessage the fallback default message
     * @param args           optional format arguments for the message
     */
    public BusinessException(Integer code, String messageCode, String defaultMessage, Object... args) {
        super(formatMessage(defaultMessage, args));
        this.code = code;
        this.messageCode = messageCode;
    }

    private static String formatMessage(String pattern, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(pattern.replace("{}", "%s"), args);
        } catch (Exception e) {
            return pattern;
        }
    }
}
