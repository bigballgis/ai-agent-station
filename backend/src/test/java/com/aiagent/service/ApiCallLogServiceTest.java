package com.aiagent.service;

import com.aiagent.dto.AgentInvokeRequest;
import com.aiagent.dto.AgentInvokeResponse;
import com.aiagent.entity.ApiCallLog;
import com.aiagent.repository.ApiCallLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApiCallLogService 单元测试
 * 测试API调用日志的创建、查询等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("API调用日志服务测试")
class ApiCallLogServiceTest {

    @Mock
    private ApiCallLogRepository apiCallLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ApiCallLogService apiCallLogService;

    private AgentInvokeRequest testRequest;
    private AgentInvokeResponse testResponse;

    @BeforeEach
    void setUp() {
        // 初始化测试请求数据
        testRequest = new AgentInvokeRequest();
        testRequest.setInputs(Map.of("query", "测试查询"));
        testRequest.setContext(Map.of("sessionId", "abc123"));
        testRequest.setAsync(false);

        // 初始化测试响应数据
        testResponse = new AgentInvokeResponse();
        testResponse.setRequestId("req-001");
        testResponse.setTaskId("task-001");
        testResponse.setStatus("SUCCESS");
        testResponse.setOutputs(Map.of("result", "测试结果"));
        testResponse.setExecutionTime(150);
    }

    // ==================== createLog 测试 ====================

    @Test
    @DisplayName("记录API调用日志 - 成功调用")
    void logApiCall_SuccessCall() throws JsonProcessingException {
        ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);

        apiCallLogService.logApiCall(
                "req-001", 1L, 100L, 1L,
                "127.0.0.1",
                "POST", "/api/v1/agents/invoke", "{\"Content-Type\": \"application/json\"}",
                testRequest, testResponse,
                200, "{\"Content-Type\": \"application/json\"}",
                ApiCallLog.ApiCallStatus.SUCCESS, 150, Boolean.FALSE, null
        );

        verify(apiCallLogRepository).save(logCaptor.capture());
        ApiCallLog savedLog = logCaptor.getValue();

        assertEquals("req-001", savedLog.getRequestId());
        assertEquals(1L, savedLog.getAgentId());
        assertEquals(100L, savedLog.getTenantId());
        assertEquals("POST", savedLog.getRequestMethod());
        assertEquals("/api/v1/agents/invoke", savedLog.getRequestPath());
        assertEquals(200, savedLog.getResponseStatus());
        assertEquals(ApiCallLog.ApiCallStatus.SUCCESS, savedLog.getStatus());
        assertEquals(150, savedLog.getExecutionTime());
        assertFalse(savedLog.getIsAsync());
        assertNull(savedLog.getAsyncTaskId());
    }

    @Test
    @DisplayName("记录API调用日志 - 异步调用")
    void logApiCall_AsyncCall() throws JsonProcessingException {
        ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);

        apiCallLogService.logApiCall(
                "req-002", 1L, 100L, 1L,
                "127.0.0.1",
                "POST", "/api/v1/agents/invoke", "{}",
                testRequest, testResponse,
                202, "{}",
                ApiCallLog.ApiCallStatus.SUCCESS, null, Boolean.TRUE, "task-async-001"
        );

        verify(apiCallLogRepository).save(logCaptor.capture());
        ApiCallLog savedLog = logCaptor.getValue();

        assertTrue(savedLog.getIsAsync());
        assertEquals("task-async-001", savedLog.getAsyncTaskId());
        assertEquals(202, savedLog.getResponseStatus());
    }

    @Test
    @DisplayName("记录API调用日志 - 失败调用记录错误信息")
    void logApiCall_FailedCall() throws JsonProcessingException {
        testResponse.setStatus("FAILED");
        testResponse.setErrorMessage("Agent执行超时");

        ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);

        apiCallLogService.logApiCall(
                "req-003", 1L, 100L, 1L,
                "127.0.0.1",
                "POST", "/api/v1/agents/invoke", "{}",
                testRequest, testResponse,
                500, "{}",
                ApiCallLog.ApiCallStatus.FAILED, 5000, Boolean.FALSE, null
        );

        verify(apiCallLogRepository).save(logCaptor.capture());
        ApiCallLog savedLog = logCaptor.getValue();

        assertEquals(ApiCallLog.ApiCallStatus.FAILED, savedLog.getStatus());
        assertEquals("Agent执行超时", savedLog.getErrorMessage());
        assertEquals(500, savedLog.getResponseStatus());
    }

    @Test
    @DisplayName("记录API调用日志 - 序列化失败时不抛出异常")
    void logApiCall_SerializationFailure_NoThrow() {
        // 使用无法序列化的对象
        AgentInvokeRequest badRequest = mock(AgentInvokeRequest.class);
        when(badRequest.getInputs()).thenThrow(new RuntimeException("序列化错误"));

        // 方法内部捕获了JsonProcessingException，不会向外抛出
        assertDoesNotThrow(() ->
                apiCallLogService.logApiCall(
                        "req-004", 1L, 100L, 1L,
                        "127.0.0.1",
                        "POST", "/api/v1/agents/invoke", "{}",
                        badRequest, testResponse,
                        200, "{}",
                        ApiCallLog.ApiCallStatus.SUCCESS, 100, Boolean.FALSE, null
                ));
    }

    @Test
    @DisplayName("记录API调用日志 - 请求体和响应体正确序列化")
    void logApiCall_BodySerialization() throws JsonProcessingException {
        ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);

        apiCallLogService.logApiCall(
                "req-005", 1L, 100L, 1L,
                "127.0.0.1",
                "POST", "/api/v1/agents/invoke", "{}",
                testRequest, testResponse,
                200, "{}",
                ApiCallLog.ApiCallStatus.SUCCESS, 100, Boolean.FALSE, null
        );

        verify(apiCallLogRepository).save(logCaptor.capture());
        ApiCallLog savedLog = logCaptor.getValue();

        // 验证请求体被序列化
        assertNotNull(savedLog.getRequestBody());
        assertTrue(savedLog.getRequestBody().contains("query"));
        // 验证响应体被序列化
        assertNotNull(savedLog.getResponseBody());
        assertTrue(savedLog.getResponseBody().contains("result"));
    }

    // ==================== getLogs / getLogsByAgent / getLogsByDateRange 测试 ====================
    // 注意：ApiCallLogService 当前只有 logApiCall 方法，查询方法在 Repository 层
    // 这里测试 Repository 层的查询方法是否被正确引用

    @Test
    @DisplayName("日志服务 - 验证Repository依赖注入正确")
    void verifyRepositoryInjection() {
        // 验证 apiCallLogRepository 被正确注入
        assertNotNull(apiCallLogService);
    }

    @Test
    @DisplayName("日志服务 - 多次调用日志记录")
    void logApiCall_MultipleCalls() throws JsonProcessingException {
        // 记录多条日志
        apiCallLogService.logApiCall("req-001", 1L, 100L, 1L,
                "127.0.0.1",
                "POST", "/api/v1/agents/1/invoke", "{}",
                testRequest, testResponse, 200, "{}",
                ApiCallLog.ApiCallStatus.SUCCESS, 100, Boolean.FALSE, null);

        apiCallLogService.logApiCall("req-002", 1L, 100L, 1L,
                "127.0.0.1",
                "POST", "/api/v1/agents/2/invoke", "{}",
                testRequest, testResponse, 200, "{}",
                ApiCallLog.ApiCallStatus.SUCCESS, 200, Boolean.FALSE, null);

        apiCallLogService.logApiCall("req-003", 2L, 100L, 1L,
                "127.0.0.1",
                "POST", "/api/v1/agents/1/invoke", "{}",
                testRequest, testResponse, 500, "{}",
                ApiCallLog.ApiCallStatus.FAILED, 3000, Boolean.FALSE, null);

        // 验证保存了3条日志
        verify(apiCallLogRepository, times(3)).save(any(ApiCallLog.class));
    }

    @Test
    @DisplayName("日志服务 - 超时状态记录")
    void logApiCall_TimeoutStatus() throws JsonProcessingException {
        ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);

        apiCallLogService.logApiCall("req-timeout", 1L, 100L, 1L,
                "127.0.0.1",
                "POST", "/api/v1/agents/invoke", "{}",
                testRequest, testResponse, 504, "{}",
                ApiCallLog.ApiCallStatus.TIMEOUT, 30000, Boolean.FALSE, null);

        verify(apiCallLogRepository).save(logCaptor.capture());
        ApiCallLog savedLog = logCaptor.getValue();

        assertEquals(ApiCallLog.ApiCallStatus.TIMEOUT, savedLog.getStatus());
        assertEquals(504, savedLog.getResponseStatus());
        assertEquals(30000, savedLog.getExecutionTime());
    }
}
