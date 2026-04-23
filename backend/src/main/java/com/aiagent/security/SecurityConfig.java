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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
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
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 认证接口：仅登录放行，其他（refresh/logout/userinfo）需要认证
                        .requestMatchers("/v1/auth/login").permitAll()
                        // Actuator 端点需要 ADMIN 角色
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
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
        return new BCryptPasswordEncoder();
    }
}
