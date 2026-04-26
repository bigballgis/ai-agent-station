package com.aiagent.controller;

import com.aiagent.security.JwtUtil;
import com.aiagent.security.ApiKeyService;
import com.aiagent.service.ApiCallAuditLogService;
import com.aiagent.service.UserService;
import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.security.JwtAuthenticationFilter;
import com.aiagent.tenant.TenantInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.aiagent.repository.UserRoleRepository;
import org.springframework.web.method.HandlerMethod;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Shared mocks for {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}
 * slices so security and logging infrastructure beans do not break context startup.
 */
public abstract class AbstractWebMvcSliceTest {

    @MockBean
    @SuppressWarnings("unused")
    protected ApiCallAuditLogService apiCallAuditLogService;

    @MockBean
    @SuppressWarnings("unused")
    protected JwtUtil jwtUtil;

    @MockBean
    @SuppressWarnings("unused")
    protected MeterRegistry meterRegistry;

    @MockBean
    @SuppressWarnings("unused")
    protected AiAgentProperties aiAgentProperties;

    @MockBean
    @SuppressWarnings("unused")
    protected ApiKeyService apiKeyService;

    @MockBean
    @SuppressWarnings("unused")
    protected UserService userService;

    @MockBean
    @SuppressWarnings("unused")
    protected UserRoleRepository userRoleRepository;

    @MockBean
    @SuppressWarnings("unused")
    protected StringRedisTemplate stringRedisTemplate;

    @MockBean
    @SuppressWarnings("unused")
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    @SuppressWarnings("unused")
    protected TenantInterceptor tenantInterceptor;

    @BeforeEach
    void allowAllRequestsPastTenantInterceptor() throws Exception {
        // Mockito mocks default boolean to false, which would abort handler execution
        // and produce 200 responses with empty bodies in MockMvc.
        lenient().when(tenantInterceptor.preHandle(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(HandlerMethod.class)
        )).thenReturn(true);
    }
}
