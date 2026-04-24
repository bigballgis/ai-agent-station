package com.aiagent.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解 - 标记需要记录审计日志的方法
 *
 * 与 @OperationLog 不同，@Audited 提供更丰富的审计语义：
 * - 支持明确的操作类型（LOGIN, LOGOUT, CREATE, UPDATE, DELETE, EXPORT, IMPORT, APPROVE, REJECT）
 * - 自动记录操作者、IP、User-Agent、请求URL
 * - 记录操作结果（成功/失败及错误信息）
 *
 * 使用示例：
 * <pre>
 *     @Audited(action = AuditAction.LOGIN, module = "认证", description = "用户登录")
 *     public Result<?> login(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /**
     * 操作类型
     */
    AuditAction action() default AuditAction.OTHER;

    /**
     * 模块名称（如 "认证", "Agent管理", "工作流", "租户管理"）
     */
    String module() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 资源类型（如 "Agent", "User", "Workflow", "Tenant"）
     */
    String resourceType() default "";
}
