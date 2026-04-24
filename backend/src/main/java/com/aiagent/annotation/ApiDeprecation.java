package com.aiagent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 废弃注解
 *
 * 标记在 Controller 方法上，表示该 API 端点已被废弃。
 * ApiDeprecationAspect 会自动在响应中添加废弃相关的 HTTP 头:
 * - X-API-Deprecated: true
 * - Sunset: <sunsetDate>
 * - Deprecation: true
 * - Link: <replacement>; rel="successor-version"
 *
 * 使用示例:
 * <pre>
 *     @ApiDeprecation(
 *         sinceVersion = "1.0",
 *         sunsetDate = "2026-06-01",
 *         replacement = "/v2/agents"
 *     )
 *     @GetMapping("/v1/agents")
 *     public Result&lt;?&gt; getAgentsV1() { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDeprecation {

    /**
     * 废弃生效的 API 版本
     */
    String sinceVersion() default "1.0";

    /**
     * 废弃日期（ISO 8601 格式，如 "2026-06-01"）
     * 此日期之后该端点可能被移除
     */
    String sunsetDate();

    /**
     * 替代端点的路径
     * 将在 Link 头中作为 successor-version 关联
     */
    String replacement() default "";

    /**
     * 废弃原因说明
     * 将在 Warning 头中返回
     */
    String message() default "This API endpoint is deprecated and will be removed.";
}
