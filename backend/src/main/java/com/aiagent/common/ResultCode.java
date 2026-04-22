package com.aiagent.common;

public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    RESOURCE_NOT_FOUND(404, "资源不存在"),

    TENANT_NOT_FOUND(1001, "租户不存在"),
    TENANT_DISABLED(1002, "租户已禁用"),
    USER_NOT_FOUND(1003, "用户不存在"),
    USER_DISABLED(1004, "用户已禁用"),
    INVALID_PASSWORD(1005, "密码错误"),
    TOKEN_INVALID(1006, "Token无效"),
    TOKEN_EXPIRED(1007, "Token已过期"),
    API_KEY_INVALID(1008, "API Key无效"),
    PERMISSION_DENIED(1009, "权限不足"),
    RESOURCE_ALREADY_EXISTS(1010, "资源已存在"),
    OPERATION_FAILED(1011, "操作失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
