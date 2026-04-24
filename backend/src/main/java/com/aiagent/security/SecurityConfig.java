package com.aiagent.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Value("${cors.allowed-origins-production:}")
    private String allowedOriginsProduction;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // 生产环境使用专用 origins 列表，禁止通配符
        boolean isProduction = activeProfiles != null && activeProfiles.contains("production");
        String originsConfig = isProduction && allowedOriginsProduction != null && !allowedOriginsProduction.isBlank()
                ? allowedOriginsProduction
                : allowedOrigins;

        List<String> origins = Arrays.asList(originsConfig.split(","));
        for (String origin : origins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                if ("*".equals(trimmed) && isProduction) {
                    // 生产环境禁止使用通配符，跳过并记录警告
                    continue;
                }
                config.addAllowedOrigin(trimmed);
            }
        }

        // 生产环境必须配置至少一个合法 origin
        if (isProduction && config.getAllowedOrigins() != null && config.getAllowedOrigins().isEmpty()) {
            throw new IllegalStateException(
                    "生产环境 CORS 配置错误: 必须通过 cors.allowed-origins-production 配置具体的允许域名，禁止使用通配符 *");
        }
        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Tenant-ID", "X-API-Key",
                "X-Request-ID", "X-API-Version", "Accept", "Origin", "Cache-Control"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setExposedHeaders(Arrays.asList("Authorization", "X-Tenant-ID", "X-Request-ID", "X-API-Version"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 认证接口：仅登录和注册放行，其他（refresh/logout/userinfo）需要认证
                        .requestMatchers("/v1/auth/login", "/v1/auth/register", "/v1/auth/captcha", "/v1/auth/refresh").permitAll()
                        // Actuator 健康检查和信息端点公开
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Actuator 其他端点需要 ADMIN 角色
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Swagger/OpenAPI 端点需要 ADMIN 角色（生产环境通过 SPRINGDOC_ENABLED=false 完全禁用）
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN")
                        // 用户管理接口需要 ADMIN 或 TENANT_ADMIN 角色
                        .requestMatchers("/v1/users/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
                        // 角色管理接口需要 ADMIN 或 TENANT_ADMIN 角色
                        .requestMatchers("/v1/roles/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
                        // 权限管理接口需要 ADMIN 或 TENANT_ADMIN 角色
                        .requestMatchers("/v1/permissions/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
                        // 租户管理接口需要 ADMIN 或 TENANT_ADMIN 角色
                        .requestMatchers("/v1/tenants/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
                        // 其他接口只需要认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
