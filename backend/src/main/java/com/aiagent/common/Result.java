package com.aiagent.common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer code;
    private String message;
    private T data;
    private String traceId;
    private String messageCode;
    private LocalDateTime timestamp;
    private String path;
    private PageResult<?> pagination;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // ==================== 通用成功方法 ====================

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ==================== 语义化工厂方法 ====================

    /**
     * POST 创建资源成功
     */
    public static <T> Result<T> created(T data) {
        return new Result<>(201, "创建成功", data);
    }

    /**
     * POST 创建资源成功（无返回数据）
     */
    public static <T> Result<T> created() {
        return new Result<>(201, "创建成功", null);
    }

    /**
     * PUT 更新资源成功
     */
    public static <T> Result<T> updated(T data) {
        return new Result<>(200, "更新成功", data);
    }

    /**
     * PUT 更新资源成功（无返回数据）
     */
    public static <T> Result<T> updated() {
        return new Result<>(200, "更新成功", null);
    }

    /**
     * DELETE 删除资源成功
     */
    public static <T> Result<T> deleted() {
        return new Result<>(200, "删除成功", null);
    }

    /**
     * 分页查询成功（自动设置 pagination 字段）
     */
    public static <T> Result<PageResult<T>> okPage(PageResult<T> pageResult) {
        Result<PageResult<T>> result = new Result<>(200, "查询成功", pageResult);
        result.setPagination(pageResult);
        return result;
    }

    // ==================== 错误方法 ====================

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    // ==================== Getter / Setter ====================

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public PageResult<?> getPagination() {
        return pagination;
    }

    public void setPagination(PageResult<?> pagination) {
        this.pagination = pagination;
    }

    // ==================== 链式调用方法 ====================

    /**
     * 从 MDC 中获取 traceId 并设置到当前 Result 对象
     */
    public Result<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * 设置 messageCode 并返回当前 Result 对象（链式调用）
     */
    public Result<T> withMessageCode(String messageCode) {
        this.messageCode = messageCode;
        return this;
    }

    /**
     * 设置 timestamp 并返回当前 Result 对象（链式调用）
     */
    public Result<T> withTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * 设置 path 并返回当前 Result 对象（链式调用）
     */
    public Result<T> withPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * 设置 pagination 并返回当前 Result 对象（链式调用）
     */
    public Result<T> withPagination(PageResult<?> pagination) {
        this.pagination = pagination;
        return this;
    }
}
