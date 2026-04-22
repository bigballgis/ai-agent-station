package com.aiagent.aspect;

import com.aiagent.annotation.OperationLog;
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
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private final SystemLogService systemLogService;

    public OperationLogAspect(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        long endTime = 0;
        boolean isSuccess = false;
        String errorMsg = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            isSuccess = true;
            endTime = System.currentTimeMillis();
            return result;
        } catch (Throwable e) {
            isSuccess = false;
            errorMsg = e.getMessage();
            endTime = System.currentTimeMillis();
            throw e;
        } finally {
            try {
                saveOperationLog(joinPoint, operationLog, startTime, endTime, isSuccess, errorMsg);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }

    private void saveOperationLog(ProceedingJoinPoint joinPoint, OperationLog operationLog, 
                                    long startTime, long endTime, boolean isSuccess, String errorMsg) {
        SystemLog systemLog = new SystemLog();
        systemLog.setTenantId(TenantContextHolder.getTenantId());
        systemLog.setModule(operationLog.module());
        systemLog.setOperation(operationLog.value());
        systemLog.setExecutionTime(endTime - startTime);
        systemLog.setIsSuccess(isSuccess);
        systemLog.setErrorMsg(errorMsg);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            systemLog.setUserId(userPrincipal.getUserId());
            systemLog.setUsername(userPrincipal.getUsername());
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        systemLog.setMethod(method.getDeclaringClass().getName() + "." + method.getName());

        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();
        StringBuilder paramsBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) paramsBuilder.append(", ");
            paramsBuilder.append(paramNames[i]).append("=").append(systemLogService.toJson(args[i]));
        }
        systemLog.setParams(paramsBuilder.toString());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            systemLog.setIp(getClientIp(request));
            systemLog.setUserAgent(request.getHeader("User-Agent"));
        }

        systemLogService.saveLog(systemLog);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
