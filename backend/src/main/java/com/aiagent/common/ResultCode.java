package com.aiagent.common;

public enum ResultCode {
    SUCCESS(200, "操作成功", "success.operation"),
    BAD_REQUEST(400, "请求参数错误", "error.validation.failed"),
    UNAUTHORIZED(401, "未授权", "error.permission.unauthorized"),
    FORBIDDEN(403, "无权限访问", "error.permission.denied"),
    NOT_FOUND(404, "资源不存在", "error.resource.not_found"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误", "error.common.operation_failed"),
    RESOURCE_NOT_FOUND(404, "资源不存在", "error.resource.not_found"),

    TENANT_NOT_FOUND(1001, "租户不存在", "error.tenant.not_found"),
    TENANT_DISABLED(1002, "租户已禁用", "error.tenant.inactive"),
    USER_NOT_FOUND(1003, "用户不存在", "error.auth.bad_credentials"),
    USER_DISABLED(1004, "用户已禁用", "error.auth.user_disabled"),
    INVALID_PASSWORD(1005, "密码错误", "error.auth.password_invalid"),
    TOKEN_INVALID(1006, "Token无效", "error.auth.token_invalid"),
    TOKEN_EXPIRED(1007, "Token已过期", "error.auth.token_expired"),
    API_KEY_INVALID(1008, "API Key无效", "error.auth.token_invalid"),
    PERMISSION_DENIED(1009, "权限不足", "error.permission.denied"),
    RESOURCE_ALREADY_EXISTS(1010, "资源已存在", "error.resource.already_exists"),
    OPERATION_FAILED(1011, "操作失败", "error.common.operation_failed"),
    TOO_MANY_REQUESTS(429, "请求过于频繁", "error.rate_limit.exceeded"),
    METHOD_NOT_ALLOWED(405, "不支持的请求方法", "error.common.unsupported_method"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型", "error.common.unsupported_media_type"),
    MISSING_PARAMETER(400, "缺少必要的请求参数", "error.common.missing_param"),
    CONSTRAINT_VIOLATION(400, "约束校验失败", "error.validation.constraint_violation"),
    VALIDATION_FAILED(400, "参数校验失败", "error.validation.failed");

    private final Integer code;
    private final String message;
    private final String messageCode;

    ResultCode(Integer code, String message, String messageCode) {
        this.code = code;
        this.message = message;
        this.messageCode = messageCode;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageCode() {
        return messageCode;
    }
}
