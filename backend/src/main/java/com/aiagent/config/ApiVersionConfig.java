package com.aiagent.config;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Configuration for API version management.
 * Registers a custom RequestMappingHandlerMapping that extracts API version
 * from URL patterns like /v1/, /v2/, etc.
 *
 * Supported versioning strategies:
 * - URL path: /api/v1/agents, /api/v2/agents
 * - Default version: v1 (when no version is specified in the URL)
 */
@Configuration
public class ApiVersionConfig implements WebMvcRegistrations {

    /** Default API version when none is specified */
    public static final String DEFAULT_VERSION = "v1";

    /** Supported API versions */
    public static final String[] SUPPORTED_VERSIONS = {"v1", "v2"};

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new ApiVersionRequestMappingHandlerMapping();
    }
}
