package com.aiagent.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * CORS 跨域配置属性
 *
 * 对应 application.yml 中 cors.* 前缀的配置项。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /** 允许的跨域来源（逗号分隔） */
    @NotBlank(message = "CORS allowed-origins 未配置，请设置环境变量 CORS_ALLOWED_ORIGINS")
    private String allowedOrigins = "http://localhost:5173,http://localhost:3000";

    /** 生产环境允许的跨域来源（逗号分隔），优先于 allowedOrigins */
    private String allowedOriginsProduction = "";
}
