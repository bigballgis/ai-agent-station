package com.aiagent.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/**
 * ETag 响应过滤器
 *
 * 功能:
 * - 对 GET 请求的响应体生成 ETag（基于 MD5 哈希）
 * - 支持 If-None-Match 头，匹配时返回 304 Not Modified
 * - 仅对配置的路径前缀生效（模板、字典、权限等低频变更数据）
 * - 为匹配的端点设置合适的 Cache-Control 头
 *
 * 性能优化:
 * - 减少不必要的网络传输（304 响应不携带响应体）
 * - 客户端可利用 ETag 进行条件请求，节省带宽
 */
@Slf4j
@Configuration
public class ETagFilterConfig {

    /**
     * 需要 ETag 支持的路径前缀（低频变更的只读数据）
     */
    private static final Set<String> ETAG_PATH_PREFIXES = Set.of(
            "/v1/agents/templates",
            "/v1/dict-types",
            "/v1/permissions"
    );

    /**
     * ETag 缓存的最大响应体大小（超过此大小不生成 ETag，避免内存压力）
     */
    private static final int MAX_ETAG_BODY_SIZE = 512 * 1024; // 512KB

    @Bean
    public FilterRegistrationBean<ETagFilter> etagFilter() {
        FilterRegistrationBean<ETagFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ETagFilter());
        registration.addUrlPatterns("/v1/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("etagFilter");
        return registration;
    }

    public static class ETagFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 仅处理 GET 请求
            if (!"GET".equalsIgnoreCase(httpRequest.getMethod())) {
                chain.doFilter(request, response);
                return;
            }

            String requestUri = httpRequest.getRequestURI();
            boolean isEtagPath = ETAG_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith);

            if (!isEtagPath) {
                chain.doFilter(request, response);
                return;
            }

            // 使用 ContentCachingResponseWrapper 捕获响应体
            ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(httpResponse);
            chain.doFilter(request, cachingResponse);

            byte[] content = cachingResponse.getContentAsByteArray();

            // 超过大小限制不生成 ETag
            if (content.length > MAX_ETAG_BODY_SIZE || content.length == 0) {
                cachingResponse.copyBodyToResponse();
                return;
            }

            // 生成 ETag
            String etag = generateETag(content);
            String ifNoneMatch = httpRequest.getHeader("If-None-Match");

            if (etag != null && etag.equals(ifNoneMatch)) {
                // ETag 匹配，返回 304
                log.debug("ETag 匹配，返回 304: uri={}, etag={}", requestUri, etag);
                httpResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                httpResponse.setHeader("ETag", etag);
                httpResponse.setHeader("Cache-Control", "private, max-age=60");
                return;
            }

            // 设置 ETag 和 Cache-Control
            httpResponse.setHeader("ETag", etag);
            httpResponse.setHeader("Cache-Control", "private, max-age=60");
            cachingResponse.copyBodyToResponse();
        }

        /**
         * 基于响应体内容生成 ETag（MD5 哈希的十六进制表示）
         */
        private String generateETag(byte[] content) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(content);
                return "\"" + HexFormat.of().formatHex(digest) + "\"";
            } catch (Exception e) {
                log.warn("生成 ETag 失败", e);
                return null;
            }
        }
    }
}
