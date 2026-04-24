package com.aiagent.exception;

import com.aiagent.common.Result;
import com.aiagent.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getTraceId() {
        return MDC.get("traceId");
    }

    private String getRequestPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(e.getMessageCode() != null ? e.getMessageCode() : ResultCode.INTERNAL_SERVER_ERROR.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("资源不存在: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.RESOURCE_NOT_FOUND.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleRateLimitException(RateLimitException e, HttpServletRequest request) {
        log.warn("请求频率限制: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.TOO_MANY_REQUESTS.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.UNAUTHORIZED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(ValidationException e, HttpServletRequest request) {
        log.warn("参数校验失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.VALIDATION_FAILED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDuplicateResourceException(DuplicateResourceException e, HttpServletRequest request) {
        log.warn("资源重复: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.RESOURCE_ALREADY_EXISTS.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(QuotaExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleQuotaExceededException(QuotaExceededException e, HttpServletRequest request) {
        log.warn("配额超限: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.TOO_MANY_REQUESTS.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(FileProcessingException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Result<Void> handleFileProcessingException(FileProcessingException e, HttpServletRequest request) {
        log.warn("文件处理失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(e.getMessageCode() != null ? e.getMessageCode() : ResultCode.FILE_PROCESSING_FAILED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限拒绝: {}", e.getMessage());
        return Result.error(ResultCode.FORBIDDEN)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.FORBIDDEN.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("参数校验失败: {}", errors);
        return Result.error(ResultCode.VALIDATION_FAILED.getCode(), ResultCode.VALIDATION_FAILED.getMessage(), errors)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.VALIDATION_FAILED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.BAD_REQUEST.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", message);
        return Result.error(ResultCode.CONSTRAINT_VIOLATION.getCode(), message)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.CONSTRAINT_VIOLATION.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.BAD_REQUEST.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.METHOD_NOT_ALLOWED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("不支持的媒体类型: {}", e.getContentType());
        return Result.error(ResultCode.UNSUPPORTED_MEDIA_TYPE)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.UNSUPPORTED_MEDIA_TYPE.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error(ResultCode.MISSING_PARAMETER)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.MISSING_PARAMETER.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.INTERNAL_SERVER_ERROR.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }
}
