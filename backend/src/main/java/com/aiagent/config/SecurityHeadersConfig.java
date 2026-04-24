package com.aiagent.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全响应头过滤器
 * 
 * 基于 OWASP Secure Headers 最佳实践:
 * - X-Content-Type-Options: nosniff — 防止 MIME Sniffing
 * - X-Frame-Options: DENY — 防止 Clickjacking
 * - X-XSS-Protection: 0 — 现代浏览器推荐禁用（依赖 CSP）
 * - Strict-Transport-Security — 强制 HTTPS
 * - Content-Security-Policy — 限制资源加载来源
 * - Referrer-Policy — 控制 Referer 头泄露
 * - Permissions-Policy — 限制浏览器 API 使用
 */
@Slf4j
@Configuration
public class SecurityHeadersConfig {

    @Value("${security.csp.connect-src-extra:}")
    private String connectSrcExtra;

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter(Environment environment) {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter(environment, connectSrcExtra));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("securityHeadersFilter");
        return registration;
    }

    public static class SecurityHeadersFilter implements Filter {

        private final Environment environment;
        private final String connectSrcExtra;

        public SecurityHeadersFilter(Environment environment, String connectSrcExtra) {
            this.environment = environment;
            this.connectSrcExtra = connectSrcExtra;
        }

        private static final Map<String, String> HEADERS = new HashMap<>();

        static {
            // 防止 MIME 类型嗅探
            HEADERS.put("X-Content-Type-Options", "nosniff");
            // 防止 Clickjacking（与 CSP frame-ancestors 'self' 保持一致）
            HEADERS.put("X-Frame-Options", "SAMEORIGIN");
            // 禁用旧的 XSS 过滤器（现代浏览器使用 CSP 替代）
            HEADERS.put("X-XSS-Protection", "0");
            // 强制 HTTPS（1年，包含子域名）
            HEADERS.put("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            // 控制 Referer 泄露
            HEADERS.put("Referrer-Policy", "strict-origin-when-cross-origin");
            // 限制浏览器 API
            HEADERS.put("Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), payment=(), usb=()");
            // 缓存控制（API 响应不缓存）
            HEADERS.put("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            HEADERS.put("Pragma", "no-cache");
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // 设置安全响应头
            HEADERS.forEach(httpResponse::setHeader);

            // CSP: 仅允许同源资源 + CDN + 连接 API
            // 注意: style-src 保留 'unsafe-inline'（Tailwind CSS 依赖内联样式）
            // TODO: 未来应引入 nonce/hash 机制替代 'unsafe-inline'，进一步提升安全性
            String connectSrc = "'self'";
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = false;
            for (String profile : activeProfiles) {
                if ("production".equals(profile)) {
                    isProduction = true;
                    break;
                }
            }
            if (!isProduction) {
                connectSrc += " http://localhost:*";
            }
            if (connectSrcExtra != null && !connectSrcExtra.isEmpty()) {
                connectSrc += " " + connectSrcExtra;
            }
            String csp = "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data: blob: https:; " +
                    "connect-src " + connectSrc + " https:; " +
                    "frame-ancestors 'self'; " +
                    "form-action 'self'; " +
                    "base-uri 'self'; " +
                    "object-src 'none'";
            httpResponse.setHeader("Content-Security-Policy", csp);

            // 移除可能暴露服务器信息的头
            httpResponse.setHeader("X-Powered-By", "");
            httpResponse.setHeader("Server", "");

            chain.doFilter(request, response);
        }
    }
}
