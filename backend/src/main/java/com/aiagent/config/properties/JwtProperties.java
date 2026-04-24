package com.aiagent.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 安全配置属性
 *
 * 对应 application.yml 中 jwt.* 前缀的配置项。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT 签名密钥（必须至少 32 个字符） */
    @NotBlank(message = "JWT 密钥未配置，请设置环境变量 JWT_SECRET")
    private String secret;

    /** Access Token 有效期（毫秒），默认 30 分钟 */
    @Min(60000)
    private long expiration = 1800000;

    /** Refresh Token 有效期（毫秒），默认 7 天 */
    @Min(3600000)
    private long refreshExpiration = 604800000;
}
