package com.aiagent.config;

import org.springframework.context.annotation.Configuration;

/**
 * CORS 跨域配置
 *
 * 注意：CORS 配置已统一在 SecurityConfig 中通过 corsConfigurationSource Bean 管理，
 * 此处的 CorsFilter Bean 与 SecurityConfig 中的配置重复，已移除。
 *
 * Spring Boot 3.x / Spring Security 6.x 兼容:
 * - 当 allowCredentials=true 时，必须使用 allowedOriginPatterns 而非 allowedOrigin
 * - 支持环境区分（开发环境宽松，生产环境严格）
 */
@Configuration
public class CorsConfig {

    // CORS 配置已迁移至 SecurityConfig.corsConfigurationSource()，此处不再重复定义 CorsFilter Bean。
}
