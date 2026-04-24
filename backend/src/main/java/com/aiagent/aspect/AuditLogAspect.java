package com.aiagent.aspect;

import com.aiagent.annotation.Audited;
import com.aiagent.annotation.AuditAction;
import com.aiagent.entity.SystemLog;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.SystemLogService;
import com.aiagent.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 审计日志 AOP 切面
 *
 * 拦截标注了 @Audited 注解的方法，自动记录审计日志到 system_logs 表。
 * 记录内容包括：
 * - 谁：userId, username, tenantId
 * - 什么：action type, module, description, resourceType
 * - 哪里：IP address, user agent, request URL
 * - 何时：timestamp（由 SystemLog 实体的 @PrePersist 自动设置）
 * - 结果：success/failure, error message, execution time
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final SystemLogService systemLogService;

    public AuditLogAspect(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean isSuccess = false;
        String errorMsg = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            isSuccess = true;
            return result;
        } catch (Throwable e) {
            isSuccess = false;
            errorMsg = truncateErrorMsg(e.getMessage());
            throw e;
        } finally {
            try {
                recordAuditLog(joinPoint, audited, startTime, isSuccess, errorMsg);
            } catch (Exception e) {
                log.error("记录审计日志失败", e);
                // 审计日志记录失败不影响主业务
            }
        }
    }

    /**
     * 记录审计日志
     */
    private void recordAuditLog(ProceedingJoinPoint joinPoint, Audited audited,
                                 long startTime, boolean isSuccess, String errorMsg) {
        SystemLog auditLog = new SystemLog();

        // 设置操作信息
        AuditAction action = audited.action();
        String description = audited.description();
        if (description == null || description.isBlank()) {
            description = action.name();
        }
        auditLog.setModule(audited.module());
        auditLog.setOperation(description);
        auditLog.setResourceType(audited.resourceType());
        auditLog.setExecutionTime(System.currentTimeMillis() - startTime);
        auditLog.setIsSuccess(isSuccess);
        auditLog.setErrorMsg(errorMsg);

        // 设置操作者信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            auditLog.setUserId(userPrincipal.getUserId());
            auditLog.setUsername(userPrincipal.getUsername());
        }
        auditLog.setTenantId(TenantContextHolder.getTenantId());

        // 设置方法信息
        Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
        auditLog.setMethod(method.getDeclaringClass().getSimpleName() + "." + method.getName());

        // 提取资源 ID
        String resourceId = extractResourceId(joinPoint);
        auditLog.setResourceId(resourceId);

        // 设置请求上下文信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            auditLog.setIp(getClientIp(request));
            auditLog.setUserAgent(truncateUserAgent(request.getHeader("User-Agent")));
        }

        // 参数记录（脱敏处理）
        auditLog.setParams(buildSafeParams(joinPoint));

        systemLogService.saveLog(auditLog);
    }

    /**
     * 从方法参数中提取资源 ID
     */
    private String extractResourceId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterNames();
        if (args == null) return null;

        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) continue;
            String name = paramNames != null && i < paramNames.length ? paramNames[i] : "";

            if ("id".equalsIgnoreCase(name) || "agentId".equalsIgnoreCase(name)
                    || "tenantId".equalsIgnoreCase(name) || "userId".equalsIgnoreCase(name)) {
                return String.valueOf(args[i]);
            }

            // 尝试从实体对象获取 ID
            try {
                java.lang.reflect.Method getIdMethod = args[i].getClass().getMethod("getId");
                Object idValue = getIdMethod.invoke(args[i]);
                if (idValue != null) {
                    return String.valueOf(idValue);
                }
            } catch (Exception ignored) {
                // 非实体对象，跳过
            }
        }
        return null;
    }

    /**
     * 构建安全的参数字符串（敏感字段脱敏）
     */
    private String buildSafeParams(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterNames();
        if (args == null) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            String paramName = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
            if (isSensitiveParam(paramName)) {
                sb.append(paramName).append("=******");
            } else {
                sb.append(paramName).append("=").append(systemLogService.toJson(args[i]));
            }
        }
        return sb.toString();
    }

    private boolean isSensitiveParam(String paramName) {
        if (paramName == null) return false;
        String lower = paramName.toLowerCase();
        return lower.contains("password") || lower.contains("secret")
                || lower.contains("token") || lower.contains("apikey")
                || lower.contains("authorization") || lower.contains("credential");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) return null;
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }

    private String truncateErrorMsg(String errorMsg) {
        if (errorMsg == null) return null;
        return errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg;
    }
}
