package com.aiagent.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 安全头配置属性
 *
 * 对应 application.yml 中 security.* 前缀的配置项（CSP 等）。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "security")
public class SecurityHeadersProperties {

    /** CSP connect-src 额外允许的来源 */
    private String cspConnectSrcExtra = "";
}
