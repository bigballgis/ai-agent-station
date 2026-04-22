package com.aiagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 跨域配置
 * 
 * Spring Boot 3.x / Spring Security 6.x 兼容:
 * - 当 allowCredentials=true 时，必须使用 allowedOriginPatterns 而非 allowedOrigin
 * - 支持环境区分（开发环境宽松，生产环境严格）
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // Spring Boot 3.x: 使用 allowedOriginPatterns 替代 allowedOrigin（兼容 credentials）
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        origins.forEach(config::addAllowedOriginPattern);

        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Tenant-ID", "X-API-Key",
                "X-Request-ID", "Accept", "Origin", "Cache-Control"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setExposedHeaders(Arrays.asList("Authorization", "X-Tenant-ID", "X-Request-ID"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
