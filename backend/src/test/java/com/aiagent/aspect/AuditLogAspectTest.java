package com.aiagent.aspect;

import com.aiagent.annotation.Audited;
import com.aiagent.annotation.AuditAction;
import com.aiagent.entity.SystemLog;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.SystemLogService;
import com.aiagent.tenant.TenantContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuditLogAspect 单元测试
 * 测试审计日志 AOP 切面功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("审计日志切面测试")
class AuditLogAspectTest {

    @Mock
    private SystemLogService systemLogService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private AuditLogAspect auditLogAspect;

    @BeforeEach
    void setUp() {
        auditLogAspect = new AuditLogAspect(systemLogService);
        SecurityContextHolder.clearContext();
        TenantContextHolder.setTenantId(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.setTenantId(null);
    }

    // ==================== 成功审计测试 ====================

    @Test
    @DisplayName("成功审计 - 方法正常执行后记录日志")
    void testAround_SuccessfulExecution_RecordsAuditLog() throws Throwable {
        // 准备 @Audited 注解
        Audited audited = createAuditedAnnotation(AuditAction.CREATE, "Agent管理", "创建Agent", "Agent");

        // 准备 joinPoint mock
        Method mockMethod = SampleService.class.getMethod("createAgent", Long.class, String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id", "name"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "test-agent"});
        when(joinPoint.proceed()).thenReturn("result");

        when(systemLogService.toJson(any())).thenReturn("{\"name\":\"test-agent\"}");

        // 设置认证信息
        UserPrincipal userPrincipal = new UserPrincipal(100L, "admin", 1L);
        Authentication auth = new UsernamePasswordAuthenticationToken(userPrincipal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContextHolder.setTenantId(1L);

        // 执行
        Object result = auditLogAspect.around(joinPoint, audited);

        assertEquals("result", result);

        // 验证审计日志被记录
        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).saveLog(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        assertEquals("Agent管理", capturedLog.getModule());
        assertEquals("创建Agent", capturedLog.getOperation());
        assertEquals("Agent", capturedLog.getResourceType());
        assertEquals(100L, capturedLog.getUserId());
        assertEquals("admin", capturedLog.getUsername());
        assertEquals(1L, capturedLog.getTenantId());
        assertTrue(capturedLog.getIsSuccess());
        assertNull(capturedLog.getErrorMsg());
        assertNotNull(capturedLog.getExecutionTime());
    }

    // ==================== 失败审计测试 ====================

    @Test
    @DisplayName("失败审计 - 方法抛出异常后记录错误日志")
    void testAround_FailedExecution_RecordsErrorAuditLog() throws Throwable {
        Audited audited = createAuditedAnnotation(AuditAction.DELETE, "Agent管理", "删除Agent", "Agent");

        Method mockMethod = SampleService.class.getMethod("deleteAgent", Long.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Agent not found"));

        UserPrincipal userPrincipal = new UserPrincipal(100L, "admin", 1L);
        Authentication auth = new UsernamePasswordAuthenticationToken(userPrincipal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContextHolder.setTenantId(1L);

        // 执行并验证异常传播
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> auditLogAspect.around(joinPoint, audited));
        assertEquals("Agent not found", exception.getMessage());

        // 验证审计日志被记录
        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).saveLog(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        assertFalse(capturedLog.getIsSuccess());
        assertEquals("Agent not found", capturedLog.getErrorMsg());
        assertEquals("Agent管理", capturedLog.getModule());
    }

    // ==================== 敏感参数脱敏测试 ====================

    @Test
    @DisplayName("敏感参数脱敏 - password 参数被掩码")
    void testAround_SensitiveParam_PasswordMasked() throws Throwable {
        Audited audited = createAuditedAnnotation(AuditAction.LOGIN, "认证", "用户登录", "");

        Method mockMethod = SampleService.class.getMethod("login", String.class, String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"username", "password"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"admin", "secret123"});
        when(joinPoint.proceed()).thenReturn("token");

        when(systemLogService.toJson(eq("admin"))).thenReturn("\"admin\"");

        auditLogAspect.around(joinPoint, audited);

        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).saveLog(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        String params = capturedLog.getParams();
        assertTrue(params.contains("username=\"admin\""));
        assertTrue(params.contains("password=******"));
    }

    @Test
    @DisplayName("敏感参数脱敏 - token 参数被掩码")
    void testAround_SensitiveParam_TokenMasked() throws Throwable {
        Audited audited = createAuditedAnnotation(AuditAction.OTHER, "API", "调用API", "");

        Method mockMethod = SampleService.class.getMethod("callApi", String.class, String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"url", "accessToken"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"https://api.example.com", "sk-1234567890"});
        when(joinPoint.proceed()).thenReturn("ok");

        when(systemLogService.toJson(eq("https://api.example.com"))).thenReturn("\"https://api.example.com\"");

        auditLogAspect.around(joinPoint, audited);

        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).saveLog(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        String params = capturedLog.getParams();
        assertTrue(params.contains("accessToken=******"));
    }

    @Test
    @DisplayName("敏感参数脱敏 - secret 参数被掩码")
    void testAround_SensitiveParam_SecretMasked() throws Throwable {
        Audited audited = createAuditedAnnotation(AuditAction.OTHER, "配置", "更新配置", "");

        Method mockMethod = SampleService.class.getMethod("updateConfig", String.class, String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"name", "clientSecret"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"config1", "my-secret-value"});
        when(joinPoint.proceed()).thenReturn("ok");

        when(systemLogService.toJson(eq("config1"))).thenReturn("\"config1\"");

        auditLogAspect.around(joinPoint, audited);

        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).saveLog(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        String params = capturedLog.getParams();
        assertTrue(params.contains("clientSecret=******"));
    }

    @Test
    @DisplayName("敏感参数脱敏 - apikey 参数被掩码")
    void testAround_SensitiveParam_ApiKeyMasked() throws Throwable {
        Audited audited = createAuditedAnnotation(AuditAction.OTHER, "配置", "设置API Key", "");

        Method mockMethod = SampleService.class.getMethod("setApiKey", String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"apikey"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"sk-proj-abc123"});
        when(joinPoint.proceed()).thenReturn("ok");

        auditLogAspect.around(joinPoint, audited);

        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).saveLog(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        String params = capturedLog.getParams();
        assertTrue(params.contains("apikey=******"));
    }

    // ==================== 无认证用户测试 ====================

    @Test
    @DisplayName("无认证用户 - 审计日志中 userId 和 username 为 null")
    void testAround_NoAuthentication_UserIdNull() throws Throwable {
        Audited audited = createAuditedAnnotation(AuditAction.CREATE, "Agent管理", "创建Agent", "Agent");

        Method mockMethod = SampleService.class.getMethod("createAgent", Long.class, String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id", "name"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "test"});
        when(joinPoint.proceed()).thenReturn("result");

        when(systemLogService.toJson(any())).thenReturn("{}");

        // 不设置认证信息
        auditLogAspect.around(joinPoint, audited);

        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).saveLog(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        assertNull(capturedLog.getUserId());
        assertNull(capturedLog.getUsername());
    }

    // ==================== 资源 ID 提取测试 ====================

    @Test
    @DisplayName("资源 ID 提取 - 从 id 参数提取")
    void testAround_ResourceId_FromIdParam() throws Throwable {
        Audited audited = createAuditedAnnotation(AuditAction.UPDATE, "Agent管理", "更新Agent", "Agent");

        Method mockMethod = SampleService.class.getMethod("updateAgent", Long.class, String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id", "name"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{42L, "updated"});
        when(joinPoint.proceed()).thenReturn("ok");

        when(systemLogService.toJson(any())).thenReturn("{}");

        auditLogAspect.around(joinPoint, audited);

        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).saveLog(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        assertEquals("42", capturedLog.getResourceId());
    }

    // ==================== 审计日志记录失败不影响业务 ====================

    @Test
    @DisplayName("审计日志记录失败 - 不影响业务方法执行")
    void testAround_AuditLogFailure_DoesNotAffectBusiness() throws Throwable {
        Audited audited = createAuditedAnnotation(AuditAction.CREATE, "Agent管理", "创建Agent", "Agent");

        Method mockMethod = SampleService.class.getMethod("createAgent", Long.class, String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id", "name"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "test"});
        when(joinPoint.proceed()).thenReturn("business-result");
        doThrow(new RuntimeException("DB error")).when(systemLogService).saveLog(any());

        Object result = auditLogAspect.around(joinPoint, audited);

        // 业务方法应正常返回
        assertEquals("business-result", result);
    }

    // ==================== 辅助方法 ====================

    private Audited createAuditedAnnotation(AuditAction action, String module, String description, String resourceType) {
        return new Audited() {
            @Override
            public AuditAction action() { return action; }
            @Override
            public String module() { return module; }
            @Override
            public String description() { return description; }
            @Override
            public String resourceType() { return resourceType; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Audited.class;
            }
        };
    }

    /**
     * 用于测试的示例服务类，提供方法签名
     */
    @SuppressWarnings("unused")
    public static class SampleService {
        public String createAgent(Long id, String name) { return "ok"; }
        public String deleteAgent(Long id) { return "ok"; }
        public String login(String username, String password) { return "ok"; }
        public String callApi(String url, String accessToken) { return "ok"; }
        public String updateConfig(String name, String clientSecret) { return "ok"; }
        public String setApiKey(String apikey) { return "ok"; }
        public String updateAgent(Long id, String name) { return "ok"; }
    }
}
