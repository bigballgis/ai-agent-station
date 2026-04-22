package com.aiagent.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom RequestCondition that extracts and matches API version from URL patterns.
 * Supports URL-based versioning with patterns like /v1/, /v2/, etc.
 *
 * Usage: Controllers can be annotated with @RequestMapping("/api/{version}/...")
 * and this condition will extract and validate the version.
 *
 * Falls back to the default version (v1) if no version is found in the URL.
 */
public class ApiVersionRequestMappingHandlerMapping
        extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionRequestMappingHandlerMapping.class);

    /** Pattern to extract version from URL path, e.g., /v1/ or /v2/ */
    private static final Pattern VERSION_PATTERN = Pattern.compile("/(v\\d+)/");

    @Override
    protected RequestCondition<?> getCustomMethodCondition(java.lang.reflect.Method method) {
        ApiVersion apiVersion = method.getDeclaringClass().getAnnotation(ApiVersion.class);
        if (apiVersion == null) {
            apiVersion = method.getAnnotation(ApiVersion.class);
        }
        if (apiVersion != null) {
            return new ApiVersionCondition(apiVersion.value());
        }
        return null;
    }

    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        ApiVersion apiVersion = handlerType.getAnnotation(ApiVersion.class);
        if (apiVersion != null) {
            return new ApiVersionCondition(apiVersion.value());
        }
        return null;
    }

    /**
     * Annotation to specify the API version for a controller or method.
     */
    @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Documented
    public @interface ApiVersion {
        /**
         * The API version(s) this controller/method supports.
         * Defaults to {"v1"}.
         */
        String[] value() default {"v1"};
    }

    /**
     * RequestCondition implementation for matching API versions.
     */
    static class ApiVersionCondition implements RequestCondition<ApiVersionCondition> {

        private final String[] versions;

        public ApiVersionCondition(String[] versions) {
            this.versions = versions;
        }

        @Override
        public ApiVersionCondition combine(ApiVersionCondition other) {
            // Merge versions from class-level and method-level annotations
            String[] merged = new String[this.versions.length + other.versions.length];
            System.arraycopy(this.versions, 0, merged, 0, this.versions.length);
            System.arraycopy(other.versions, 0, merged, this.versions.length, other.versions.length);
            return new ApiVersionCondition(merged);
        }

        @Override
        public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
            String requestVersion = extractVersion(request);
            if (requestVersion == null) {
                requestVersion = ApiVersionConfig.DEFAULT_VERSION;
            }

            for (String supportedVersion : versions) {
                if (supportedVersion.equals(requestVersion)) {
                    return this;
                }
            }

            // If no exact match, check if default version is supported
            for (String supportedVersion : versions) {
                if (supportedVersion.equals(ApiVersionConfig.DEFAULT_VERSION)) {
                    return this;
                }
            }

            log.debug("No matching API version found. Request version: {}, Supported: {}",
                    requestVersion, String.join(", ", versions));
            return null;
        }

        @Override
        public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
            // Prefer more specific version matches
            String requestVersion = extractVersion(request);
            if (requestVersion == null) {
                requestVersion = ApiVersionConfig.DEFAULT_VERSION;
            }

            boolean thisMatches = containsVersion(this.versions, requestVersion);
            boolean otherMatches = containsVersion(other.versions, requestVersion);

            if (thisMatches && !otherMatches) {
                return -1; // This condition is more specific
            } else if (!thisMatches && otherMatches) {
                return 1;  // Other condition is more specific
            }

            return 0;
        }

        /**
         * Extract the API version from the request URL.
         * Looks for patterns like /v1/, /v2/, etc.
         *
         * @param request the HTTP request
         * @return the version string (e.g., "v1") or null if not found
         */
        private String extractVersion(HttpServletRequest request) {
            String path = request.getRequestURI();
            if (path == null) {
                return null;
            }

            Matcher matcher = VERSION_PATTERN.matcher(path);
            if (matcher.find()) {
                String version = matcher.group(1);
                // Validate against supported versions
                for (String supported : ApiVersionConfig.SUPPORTED_VERSIONS) {
                    if (supported.equals(version)) {
                        return version;
                    }
                }
                log.warn("Unsupported API version requested: {}", version);
            }

            return null;
        }

        private boolean containsVersion(String[] versions, String version) {
            for (String v : versions) {
                if (v.equals(version)) {
                    return true;
                }
            }
            return false;
        }

        public String[] getVersions() {
            return versions;
        }
    }
}
