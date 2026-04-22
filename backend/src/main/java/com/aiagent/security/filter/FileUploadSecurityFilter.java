package com.aiagent.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.IOException;

/**
 * Servlet filter that sanitizes file upload request parameters for security.
 * Removes null bytes from filenames and normalizes paths to prevent
 * path traversal and injection attacks.
 *
 * This filter can be selectively applied to upload endpoints via FilterRegistration.
 */
public class FileUploadSecurityFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            chain.doFilter(new SanitizedRequestWrapper(httpRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Request wrapper that sanitizes filenames in multipart requests.
     */
    static class SanitizedRequestWrapper extends HttpServletRequestWrapper {

        public SanitizedRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            // Sanitize Content-Disposition header which may contain filenames
            if (name != null && name.equalsIgnoreCase("Content-Disposition")) {
                return sanitizeFilename(value);
            }
            return value;
        }

        @Override
        public String getRequestURI() {
            return normalizePath(super.getRequestURI());
        }

        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer(normalizePath(super.getRequestURL().toString()));
        }

        @Override
        public String getServletPath() {
            return normalizePath(super.getServletPath());
        }

        @Override
        public String getPathInfo() {
            String pathInfo = super.getPathInfo();
            return pathInfo != null ? normalizePath(pathInfo) : null;
        }

        @Override
        public String getPathTranslated() {
            String pathTranslated = super.getPathTranslated();
            return pathTranslated != null ? normalizePath(pathTranslated) : null;
        }

        // ==================== Sanitization Methods ====================

        /**
         * Remove null bytes and trim whitespace from parameter values.
         */
        private String sanitize(String value) {
            if (value == null) {
                return null;
            }
            // Remove null bytes
            String sanitized = value.replace("\0", "");
            return sanitized.trim();
        }

        /**
         * Sanitize filenames in Content-Disposition headers.
         * Removes null bytes, normalizes paths, and removes path traversal sequences.
         */
        private String sanitizeFilename(String value) {
            if (value == null) {
                return null;
            }
            // Remove null bytes
            String sanitized = value.replace("\0", "");

            // Remove path traversal sequences from filename portion
            sanitized = sanitized.replace("../", "").replace("..\\", "");
            sanitized = sanitized.replace("/..", "").replace("\\..", "");

            return sanitized;
        }

        /**
         * Normalize a file path by removing redundant separators and resolving relative paths.
         */
        private String normalizePath(String path) {
            if (path == null) {
                return null;
            }
            // Remove null bytes
            path = path.replace("\0", "");

            // Replace backslashes with forward slashes
            path = path.replace('\\', '/');

            // Collapse multiple slashes into one
            while (path.contains("//")) {
                path = path.replace("//", "/");
            }

            // Remove trailing slashes (except for root)
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            return path;
        }
    }
}
