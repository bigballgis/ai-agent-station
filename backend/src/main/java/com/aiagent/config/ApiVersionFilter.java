package com.aiagent.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API 版本过滤器
 *
 * 从请求头 X-API-Version 中提取 API 版本号，设置到请求属性中，
 * 并在响应头中返回当前 API 版本。
 *
 * 支持的版本值: 1, 2
 * 默认版本: 1（未指定时）
 */
@Component
public class ApiVersionFilter implements Filter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionFilter.class);

    /** 请求头名称 */
    public static final String API_VERSION_HEADER = "X-API-Version";

    /** 请求属性键名 */
    public static final String API_VERSION_ATTRIBUTE = "apiVersion";

    /** 默认版本 */
    public static final int DEFAULT_VERSION = 1;

    /** 支持的版本 */
    public static final int[] SUPPORTED_VERSIONS = {1, 2};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        int version = resolveVersion(httpRequest);

        // 将版本号设置到请求属性中，供后续处理器使用
        httpRequest.setAttribute(API_VERSION_ATTRIBUTE, version);

        // 在响应头中返回当前 API 版本
        httpResponse.setHeader(API_VERSION_HEADER, String.valueOf(version));

        log.debug("API version resolved: {} for request: {} {}",
                version, httpRequest.getMethod(), httpRequest.getRequestURI());

        chain.doFilter(request, response);
    }

    /**
     * 从请求头解析 API 版本号
     *
     * @param request HTTP 请求
     * @return 版本号（1 或 2），未指定或无效时返回默认版本 1
     */
    private int resolveVersion(HttpServletRequest request) {
        String versionHeader = request.getHeader(API_VERSION_HEADER);

        if (versionHeader == null || versionHeader.isBlank()) {
            return DEFAULT_VERSION;
        }

        try {
            int version = Integer.parseInt(versionHeader.trim());
            for (int supported : SUPPORTED_VERSIONS) {
                if (supported == version) {
                    return version;
                }
            }
            log.warn("Unsupported API version requested: {}, falling back to default: {}",
                    versionHeader, DEFAULT_VERSION);
            return DEFAULT_VERSION;
        } catch (NumberFormatException e) {
            log.warn("Invalid API version header value: '{}', falling back to default: {}",
                    versionHeader, DEFAULT_VERSION);
            return DEFAULT_VERSION;
        }
    }

    @Override
    public int getOrder() {
        // 在 TraceFilter (HIGHEST_PRECEDENCE) 之后执行
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
