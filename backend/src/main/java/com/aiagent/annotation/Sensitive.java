package com.aiagent.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;

import java.lang.annotation.*;

/**
 * 敏感数据注解 - 标记字段为敏感数据，在 API 响应和日志中自动脱敏
 *
 * 使用方式：
 * <pre>
 *     {@code @Sensitive} private String password;
 *     {@code @Sensitive(maskPrefix = 4, maskSuffix = 4)} private String apiKey;
 * </pre>
 *
 * 脱敏规则：
 * - 密码类字段：完全掩码为 "******"
 * - Token/API Key 类字段：保留前4位和后4位，中间用 **** 替代
 * - 可通过 maskPrefix 和 maskSuffix 自定义保留位数
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
public @interface Sensitive {

    /**
     * 脱敏类型
     */
    SensitiveType type() default SensitiveType.PASSWORD;

    /**
     * 自定义掩码前缀保留字符数（仅对 PARTIAL 类型生效）
     */
    int maskPrefix() default 4;

    /**
     * 自定义掩码后缀保留字符数（仅对 PARTIAL 类型生效）
     */
    int maskSuffix() default 4;
}
