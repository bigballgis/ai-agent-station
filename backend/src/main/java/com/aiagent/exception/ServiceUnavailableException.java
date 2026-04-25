package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 服务不可用异常 (503)
 *
 * 用于外部服务（LLM、MCP、第三方API等）不可用或连接失败的场景。
 * 包含服务名称和原因等上下文信息。
 */
public class ServiceUnavailableException extends BusinessException {

    /** 不可用的服务名称 */
    private final String serviceName;

    /** 不可用的原因 */
    private final String reason;

    public ServiceUnavailableException(String message) {
        super(ResultCode.SERVICE_UNAVAILABLE.getCode(), ResultCode.SERVICE_UNAVAILABLE.getMessageCode(), message);
        this.serviceName = null;
        this.reason = null;
    }

    public ServiceUnavailableException(String serviceName, String reason) {
        super(ResultCode.SERVICE_UNAVAILABLE.getCode(),
                ResultCode.SERVICE_UNAVAILABLE.getMessageCode(),
                "服务暂不可用: " + serviceName + (reason != null ? " (" + reason + ")" : ""));
        this.serviceName = serviceName;
        this.reason = reason;
    }

    public ServiceUnavailableException(String serviceName, String reason, Throwable cause) {
        super(ResultCode.SERVICE_UNAVAILABLE.getCode(),
                ResultCode.SERVICE_UNAVAILABLE.getMessageCode(),
                "服务暂不可用: " + serviceName + (reason != null ? " (" + reason + ")" : ""));
        initCause(cause);
        this.serviceName = serviceName;
        this.reason = reason;
    }

    public ServiceUnavailableException() {
        super(ResultCode.SERVICE_UNAVAILABLE);
        this.serviceName = null;
        this.reason = null;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getReason() {
        return reason;
    }
}
