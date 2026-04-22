package com.aiagent.security;

import lombok.RequiredArgsConstructor;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 认证接口放行
                        .requestMatchers("/auth/**").permitAll()
                        // Actuator 端点需要 ADMIN 角色
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // 用户管理接口需要 ADMIN 或 TENANT_ADMIN 角色
                        .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
                        // 角色管理接口需要 ADMIN 或 TENANT_ADMIN 角色
                        .requestMatchers("/api/v1/roles/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
                        // 权限管理接口需要 ADMIN 或 TENANT_ADMIN 角色
                        .requestMatchers("/api/v1/permissions/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
                        // 租户管理接口需要 ADMIN 或 TENANT_ADMIN 角色
                        .requestMatchers("/api/v1/tenants/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
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
