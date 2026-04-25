package com.aiagent.exception;

import com.aiagent.common.Result;
import com.aiagent.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private String getRequestMethod(HttpServletRequest request) {
        return request.getMethod();
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    // ==================== 自定义业务异常 ====================

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(e.getMessageCode() != null ? e.getMessageCode() : ResultCode.INTERNAL_SERVER_ERROR.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("资源不存在: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.RESOURCE_NOT_FOUND.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleRateLimitException(RateLimitException e, HttpServletRequest request) {
        log.warn("请求频率限制: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.TOO_MANY_REQUESTS.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleRateLimitExceededException(RateLimitExceededException e, HttpServletRequest request,
                                                          HttpServletResponse response) {
        log.warn("API速率限制超出: {}, limit={}, remaining={}, resetTime={} [{} {}]",
                e.getMessage(), e.getLimit(), e.getRemaining(), e.getResetTime(),
                getRequestMethod(request), getRequestPath(request));

        // 写入标准速率限制响应头
        response.setHeader("X-RateLimit-Limit", String.valueOf(e.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(e.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(e.getResetTime()));
        response.setHeader("Retry-After", String.valueOf(e.getWindowSeconds()));

        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.TOO_MANY_REQUESTS.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证失败: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.UNAUTHORIZED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(ValidationException e, HttpServletRequest request) {
        log.warn("参数校验失败: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.VALIDATION_FAILED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDuplicateResourceException(DuplicateResourceException e, HttpServletRequest request) {
        log.warn("资源重复: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.RESOURCE_ALREADY_EXISTS.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(QuotaExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleQuotaExceededException(QuotaExceededException e, HttpServletRequest request) {
        log.warn("配额超限: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.TOO_MANY_REQUESTS.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(FileProcessingException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Result<Void> handleFileProcessingException(FileProcessingException e, HttpServletRequest request) {
        log.warn("文件处理失败: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(e.getMessageCode() != null ? e.getMessageCode() : ResultCode.FILE_PROCESSING_FAILED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    // ==================== Round 291: 新增自定义异常处理器 ====================

    @ExceptionHandler(ConcurrentModificationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleConcurrentModificationException(ConcurrentModificationException e, HttpServletRequest request) {
        log.warn("数据并发冲突: entityType={}, entityId={} [{} {}]",
                e.getEntityType(), e.getEntityId(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.CONCURRENT_MODIFICATION.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleServiceUnavailableException(ServiceUnavailableException e, HttpServletRequest request) {
        log.warn("服务不可用: serviceName={}, reason={} [{} {}]",
                e.getServiceName(), e.getReason(), getRequestMethod(request), getRequestPath(request));
        return Result.error(e.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.SERVICE_UNAVAILABLE.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    // ==================== Spring Security 异常 ====================

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限拒绝: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.FORBIDDEN)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.FORBIDDEN.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, Object> groupedErrors = groupFieldErrors(e);
        log.warn("参数校验失败: {} [{} {}]", groupedErrors, getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.VALIDATION_FAILED.getCode(), ResultCode.VALIDATION_FAILED.getMessage(), groupedErrors)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.VALIDATION_FAILED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, Object>> handleBindException(BindException e, HttpServletRequest request) {
        Map<String, Object> groupedErrors = groupFieldErrorsFromBindingResult(e.getBindingResult());
        log.warn("参数绑定失败: {} [{} {}]", groupedErrors, getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), groupedErrors)
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
        log.warn("约束校验失败: {} [{} {}]", message, getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.CONSTRAINT_VIOLATION.getCode(), message)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.CONSTRAINT_VIOLATION.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: {} [{} {}]", e.getMessage(), getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.BAD_REQUEST.getCode(), e.getMessage())
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.BAD_REQUEST.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    // ==================== Round 292: 新增 Spring MVC 异常处理器 ====================

    /**
     * 路径参数/查询参数类型不匹配（如 ?id=abc 期望 int）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String paramName = e.getName();
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        String value = e.getValue() != null ? e.getValue().toString() : "null";
        String message = "参数 '" + paramName + "' 类型错误: 期望 " + requiredType + ", 实际值 '" + value + "'";
        log.warn("参数类型不匹配: {} [{} {}]", message, getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.PARAMETER_TYPE_MISMATCH.getCode(), message)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.PARAMETER_TYPE_MISMATCH.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    /**
     * 文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        long maxSize = e.getMaxUploadSize();
        String message = "文件上传大小超限, 最大允许 " + (maxSize / 1024 / 1024) + "MB";
        log.warn("文件上传大小超限: maxSize={}MB [{} {}]", maxSize / 1024 / 1024,
                getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.FILE_UPLOAD_SIZE_EXCEEDED.getCode(), message)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.FILE_UPLOAD_SIZE_EXCEEDED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    /**
     * 不支持的 HTTP 请求方法
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String message = "不支持的请求方法: " + e.getMethod()
                + ", 支持: " + String.join(", ", e.getSupportedMethods());
        log.warn("不支持的请求方法: {} [{} {}]", message, e.getMethod(), getRequestPath(request));
        return Result.error(ResultCode.METHOD_NOT_ALLOWED.getCode(), message)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.METHOD_NOT_ALLOWED.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    /**
     * 不支持的 Content-Type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        String message = "不支持的媒体类型: " + e.getContentType()
                + ", 支持: " + String.join(", ", e.getSupportedMediaTypes().stream()
                .map(mt -> mt.toString()).collect(Collectors.toList()));
        log.warn("不支持的媒体类型: {} [{} {}]", e.getContentType(), getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.UNSUPPORTED_MEDIA_TYPE.getCode(), message)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.UNSUPPORTED_MEDIA_TYPE.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    /**
     * 请求体不可读（JSON 格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        String message = "请求体格式错误: " + (e.getMostSpecificCause().getMessage() != null
                ? e.getMostSpecificCause().getMessage() : e.getMessage());
        log.warn("请求体格式错误: {} [{} {}]", message, getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.BAD_REQUEST.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数: {} [{} {}]", e.getParameterName(), getRequestMethod(request), getRequestPath(request));
        return Result.error(ResultCode.MISSING_PARAMETER)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.MISSING_PARAMETER.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    // ==================== 兜底异常处理器 ====================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{} {}]", getRequestMethod(request), getRequestPath(request), e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR)
                .withTraceId(getTraceId())
                .withMessageCode(ResultCode.INTERNAL_SERVER_ERROR.getMessageCode())
                .withTimestamp(now())
                .withPath(getRequestPath(request));
    }

    // ==================== 验证错误分组格式化工具 ====================

    /**
     * 将 MethodArgumentNotValidException 的字段错误按字段名分组。
     * <p>
     * 同一字段可能有多个校验错误，格式为:
     * {
     *   "fieldName": ["错误1", "错误2"],
     *   ...
     * }
     */
    private Map<String, Object> groupFieldErrors(MethodArgumentNotValidException e) {
        return groupFieldErrorsFromBindingResult(e.getBindingResult());
    }

    /**
     * 将 BindingResult 的字段错误按字段名分组。
     * <p>
     * 同一字段可能有多个校验错误，格式为:
     * {
     *   "fieldName": ["错误1", "错误2"],
     *   ...
     * }
     */
    private Map<String, Object> groupFieldErrorsFromBindingResult(org.springframework.validation.BindingResult bindingResult) {
        Map<String, List<String>> grouped = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            grouped.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        // 如果每个字段只有一个错误，直接用 String 而非 List，保持简洁
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            if (entry.getValue().size() == 1) {
                result.put(entry.getKey(), entry.getValue().get(0));
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
