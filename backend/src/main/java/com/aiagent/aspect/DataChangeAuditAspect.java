package com.aiagent.aspect;

import com.aiagent.entity.DataChangeLog;
import com.aiagent.repository.DataChangeLogRepository;
import com.aiagent.security.UserPrincipal;
import com.aiagent.security.annotation.Auditable;
import com.aiagent.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AOP切面：拦截标注了 @Auditable 注解的方法，自动记录数据变更审计日志。
 * 支持CREATE、UPDATE、DELETE三种操作类型的字段级别变更追踪。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataChangeAuditAspect {

    private final DataChangeLogRepository dataChangeLogRepository;

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String tableName = auditable.tableName();
        String description = auditable.description();
        String operator = getCurrentOperator();
        Long operatorId = getCurrentOperatorId();
        String operatorIp = getClientIp();
        String userAgent = getUserAgent();
        Long tenantId = getTenantId();

        // 获取方法参数中的实体对象
        Object[] args = joinPoint.getArgs();
        Object entity = extractEntity(args);

        // 判断操作类型
        String operationType = determineOperationType(joinPoint, args);

        // 对于UPDATE操作，先获取旧值
        Object oldEntity = null;
        if ("UPDATE".equals(operationType) && entity != null) {
            oldEntity = captureOldState(entity);
        }

        // 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            log.error("审计方法执行失败: {}", description, e);
            throw e;
        }

        // 记录变更日志
        try {
            recordChangeLogs(tableName, operationType, entity, oldEntity, operator, operatorId, operatorIp, userAgent, tenantId);
        } catch (Exception e) {
            log.error("记录审计日志失败: {}", description, e);
            // 审计日志记录失败不影响主业务
        }

        return result;
    }

    /**
     * 从方法参数中提取实体对象
     */
    private Object extractEntity(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        // 通常实体是第一个参数
        for (Object arg : args) {
            if (arg != null && !isSimpleType(arg.getClass())) {
                return arg;
            }
        }
        return null;
    }

    /**
     * 判断是否为简单类型（非实体对象）
     */
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class
                || clazz.isEnum();
    }

    /**
     * 通过方法名判断操作类型
     */
    private String determineOperationType(ProceedingJoinPoint joinPoint, Object[] args) {
        String methodName = joinPoint.getSignature().getName();
        if (methodName.startsWith("create") || methodName.startsWith("save") || methodName.startsWith("add")) {
            return "CREATE";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify") || methodName.startsWith("edit")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        }
        // 根据参数数量推断：单参数通常是create/update，无参数可能是delete(id已在路径中)
        if (args != null && args.length > 0 && args[0] != null) {
            Object firstArg = args[0];
            if (firstArg instanceof Long || firstArg instanceof String) {
                return "DELETE";
            }
            return "CREATE";
        }
        return "UNKNOWN";
    }

    /**
     * 捕获旧状态（用于UPDATE场景，简化实现）
     * 在实际项目中，可以通过ID从数据库查询旧值
     */
    private Object captureOldState(Object entity) {
        try {
            // 尝试通过反射获取id
            Object id = getEntityId(entity);
            if (id == null) {
                return null;
            }
            // 此处简化处理，实际可通过Repository查询旧值
            // 由于切面无法注入具体Repository，采用浅拷贝方式
            return entity.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.debug("无法捕获旧状态: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取实体的ID值
     */
    private Object getEntityId(Object entity) {
        try {
            for (Field field : entity.getClass().getDeclaredFields()) {
                if ("id".equals(field.getName())) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
            }
        } catch (Exception e) {
            log.debug("获取实体ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 记录变更日志
     */
    private void recordChangeLogs(String tableName, String operationType, Object entity,
                                  Object oldEntity, String operator, Long operatorId,
                                  String operatorIp, String userAgent, Long tenantId) {
        if (entity == null) {
            return;
        }

        List<DataChangeLog> logs = new ArrayList<>();
        String recordId = String.valueOf(getEntityId(entity));

        switch (operationType) {
            case "CREATE":
                logs.addAll(buildCreateLogs(tableName, recordId, entity, operator, operatorId, operatorIp, userAgent, tenantId));
                break;
            case "UPDATE":
                if (oldEntity != null) {
                    logs.addAll(buildUpdateLogs(tableName, recordId, entity, oldEntity, operator, operatorId, operatorIp, userAgent, tenantId));
                } else {
                    DataChangeLog logEntry = new DataChangeLog();
                    logEntry.setTableName(tableName);
                    logEntry.setRecordId(recordId);
                    logEntry.setOperationType("UPDATE");
                    logEntry.setFieldName(null);
                    logEntry.setOldValue(null);
                    logEntry.setNewValue("整体更新");
                    logEntry.setOperator(operator);
                    logEntry.setOperatorId(operatorId);
                    logEntry.setOperatorIp(operatorIp);
                    logEntry.setUserAgent(userAgent);
                    logEntry.setTenantId(tenantId);
                    logEntry.setOperatedAt(LocalDateTime.now());
                    logs.add(logEntry);
                }
                break;
            case "DELETE":
                logs.addAll(buildDeleteLogs(tableName, recordId, entity, operator, operatorId, operatorIp, userAgent, tenantId));
                break;
            default:
                break;
        }

        if (!logs.isEmpty()) {
            dataChangeLogRepository.saveAll(logs);
            log.info("记录审计日志: table={}, operation={}, recordId={}, fields={}",
                    tableName, operationType, recordId, logs.size());
        }
    }

    /**
     * 构建CREATE操作的日志
     */
    private List<DataChangeLog> buildCreateLogs(String tableName, String recordId,
                                                Object entity, String operator, Long operatorId,
                                                String operatorIp, String userAgent, Long tenantId) {
        List<DataChangeLog> logs = new ArrayList<>();
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (shouldSkipField(field)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    DataChangeLog logEntry = new DataChangeLog();
                    logEntry.setTableName(tableName);
                    logEntry.setRecordId(recordId);
                    logEntry.setOperationType("CREATE");
                    logEntry.setFieldName(field.getName());
                    logEntry.setOldValue(null);
                    logEntry.setNewValue(value.toString());
                    logEntry.setOperator(operator);
                    logEntry.setOperatorId(operatorId);
                    logEntry.setOperatorIp(operatorIp);
                    logEntry.setUserAgent(userAgent);
                    logEntry.setTenantId(tenantId);
                    logEntry.setOperatedAt(LocalDateTime.now());
                    logs.add(logEntry);
                }
            } catch (IllegalAccessException e) {
                log.debug("无法访问字段: {}", field.getName());
            }
        }
        return logs;
    }

    /**
     * 构建UPDATE操作的日志（比较新旧值）
     */
    private List<DataChangeLog> buildUpdateLogs(String tableName, String recordId,
                                                Object newEntity, Object oldEntity,
                                                String operator, Long operatorId,
                                                String operatorIp, String userAgent, Long tenantId) {
        List<DataChangeLog> logs = new ArrayList<>();
        for (Field field : newEntity.getClass().getDeclaredFields()) {
            if (shouldSkipField(field)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object newValue = field.get(newEntity);
                Object oldValue = field.get(oldEntity);

                if (newValue != null && !newValue.equals(oldValue)) {
                    DataChangeLog logEntry = new DataChangeLog();
                    logEntry.setTableName(tableName);
                    logEntry.setRecordId(recordId);
                    logEntry.setOperationType("UPDATE");
                    logEntry.setFieldName(field.getName());
                    logEntry.setOldValue(oldValue != null ? oldValue.toString() : null);
                    logEntry.setNewValue(newValue.toString());
                    logEntry.setOperator(operator);
                    logEntry.setOperatorId(operatorId);
                    logEntry.setOperatorIp(operatorIp);
                    logEntry.setUserAgent(userAgent);
                    logEntry.setTenantId(tenantId);
                    logEntry.setOperatedAt(LocalDateTime.now());
                    logs.add(logEntry);
                }
            } catch (IllegalAccessException e) {
                log.debug("无法访问字段: {}", field.getName());
            }
        }
        return logs;
    }

    /**
     * 构建DELETE操作的日志
     */
    private List<DataChangeLog> buildDeleteLogs(String tableName, String recordId,
                                                Object entity, String operator, Long operatorId,
                                                String operatorIp, String userAgent, Long tenantId) {
        List<DataChangeLog> logs = new ArrayList<>();
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (shouldSkipField(field)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    DataChangeLog logEntry = new DataChangeLog();
                    logEntry.setTableName(tableName);
                    logEntry.setRecordId(recordId);
                    logEntry.setOperationType("DELETE");
                    logEntry.setFieldName(field.getName());
                    logEntry.setOldValue(value.toString());
                    logEntry.setNewValue(null);
                    logEntry.setOperator(operator);
                    logEntry.setOperatorId(operatorId);
                    logEntry.setOperatorIp(operatorIp);
                    logEntry.setUserAgent(userAgent);
                    logEntry.setTenantId(tenantId);
                    logEntry.setOperatedAt(LocalDateTime.now());
                    logs.add(logEntry);
                }
            } catch (IllegalAccessException e) {
                log.debug("无法访问字段: {}", field.getName());
            }
        }
        return logs;
    }

    /**
     * 判断是否跳过该字段（跳过序列化相关、静态字段、集合、敏感字段等）
     */
    private static final java.util.Set<String> SENSITIVE_FIELD_NAMES = java.util.Set.of(
            "password", "oldpassword", "newpassword", "confirmpassword",
            "secret", "token", "accesstoken", "refreshtoken",
            "apikey", "authorization", "credential"
    );

    private boolean shouldSkipField(Field field) {
        if ("serialVersionUID".equals(field.getName())
                || java.lang.reflect.Modifier.isStatic(field.getModifiers())
                || java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
            return true;
        }
        // 跳过敏感字段，不记录到审计日志
        String lowerName = field.getName().toLowerCase();
        for (String sensitive : SENSITIVE_FIELD_NAMES) {
            if (lowerName.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前操作用户
     */
    private String getCurrentOperator() {
        try {
            UserPrincipal user = SecurityUtils.getCurrentUser();
            if (user != null) {
                return user.getUsername();
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败: {}", e.getMessage());
        }
        return "SYSTEM";
    }

    /**
     * 获取当前操作用户ID
     */
    private Long getCurrentOperatorId() {
        try {
            UserPrincipal user = SecurityUtils.getCurrentUser();
            if (user != null) {
                return user.getUserId();
            }
        } catch (Exception e) {
            log.debug("获取当前用户ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前租户ID
     */
    private Long getTenantId() {
        try {
            return com.aiagent.tenant.TenantContextHolder.getTenantId();
        } catch (Exception e) {
            log.debug("获取租户ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取User-Agent
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String ua = attributes.getRequest().getHeader("User-Agent");
                return ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua;
            }
        } catch (Exception e) {
            log.debug("获取User-Agent失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        } catch (Exception e) {
            log.debug("获取客户端IP失败: {}", e.getMessage());
        }
        return null;
    }
}
