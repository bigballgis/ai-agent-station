package com.aiagent.aspect;

import com.aiagent.annotation.ApiDeprecation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * API 废弃切面
 *
 * 拦截标注了 @ApiDeprecation 注解的 Controller 方法，
 * 在 HTTP 响应中自动添加废弃相关的头部信息:
 *
 * - X-API-Deprecated: true
 * - Sunset: <sunsetDate>
 * - Deprecation: true
 * - Warning: 299 - "Deprecated API"
 * - Link: <replacement>; rel="successor-version"（如果有替代端点）
 *
 * 同时在日志中记录废弃端点的调用，便于追踪迁移进度。
 */
@Slf4j
@Aspect
@Component
public class ApiDeprecationAspect {

    /**
     * 在标注了 @ApiDeprecation 的方法正常返回后，添加废弃响应头
     */
    @AfterReturning("@annotation(apiDeprecation)")
    public void addDeprecationHeaders(JoinPoint joinPoint, ApiDeprecation apiDeprecation) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            return;
        }

        // 添加废弃标准头
        response.setHeader("X-API-Deprecated", "true");
        response.setHeader("Deprecation", "true");

        // 添加 Sunset 头（RFC 8594）
        String sunsetDate = apiDeprecation.sunsetDate();
        if (sunsetDate != null && !sunsetDate.isEmpty()) {
            response.setHeader("Sunset", sunsetDate);
        }

        // 添加 Warning 头（RFC 7234）
        String message = apiDeprecation.message();
        if (message != null && !message.isEmpty()) {
            response.setHeader("Warning", "299 - \"" + message + "\"");
        }

        // 添加 Link 头指向替代端点
        String replacement = apiDeprecation.replacement();
        if (replacement != null && !replacement.isEmpty()) {
            response.setHeader("Link", "<" + replacement + ">; rel=\"successor-version\"");
        }

        // 记录日志，便于追踪废弃端点的调用情况
        String methodName = joinPoint.getSignature().toShortString();
        log.info("[ApiDeprecation] 废弃端点被调用: method={}, sinceVersion={}, sunsetDate={}, replacement={}",
                methodName, apiDeprecation.sinceVersion(), sunsetDate, replacement);
    }
}
