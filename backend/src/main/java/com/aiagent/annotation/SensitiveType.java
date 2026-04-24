package com.aiagent.annotation;

/**
 * 敏感数据脱敏类型
 */
public enum SensitiveType {
    /**
     * 完全掩码（密码类）: "******"
     */
    PASSWORD,
    /**
     * 部分掩码（API Key、Token类）: 保留前N位和后M位
     */
    PARTIAL,
    /**
     * 邮箱掩码: u***@example.com
     */
    EMAIL,
    /**
     * 手机号掩码: 138****1234
     */
    PHONE
}
