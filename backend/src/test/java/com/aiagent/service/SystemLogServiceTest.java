package com.aiagent.service;

import com.aiagent.common.PageResult;
import com.aiagent.entity.SystemLog;
import com.aiagent.repository.SystemLogRepository;
import com.aiagent.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SystemLogService 单元测试
 * 测试系统日志服务的核心方法：保存日志、查询日志、按模块查询、JSON序列化等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("系统日志服务测试")
class SystemLogServiceTest {

    @Mock
    private SystemLogRepository systemLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SystemLogService systemLogService;

    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    private static final Long TENANT_ID = 1L;
    private static final Long LOG_ID = 10L;

    @BeforeEach
    void setUp() {
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    // ==================== saveLog 测试 ====================

    @Test
    @DisplayName("保存日志 - 成功保存日志")
    void saveLog_shouldSaveSuccessfully() {
        // 准备测试数据
        SystemLog log = new SystemLog();
        log.setId(LOG_ID);
        log.setModule("Agent");
        log.setOperation("创建Agent");

        // 执行测试
        systemLogService.saveLog(log);

        // 验证结果
        verify(systemLogRepository).save(log);
    }

    @Test
    @DisplayName("保存日志 - 保存失败时不抛出异常（静默处理）")
    void saveLog_shouldNotThrowWhenSaveFails() {
        // 准备测试数据
        SystemLog log = new SystemLog();
        log.setModule("Agent");

        // 模拟保存失败
        doThrow(new RuntimeException("数据库异常")).when(systemLogRepository).save(any());

        // 执行测试 - 不应抛出异常
        assertDoesNotThrow(() -> systemLogService.saveLog(log));
    }

    // ==================== getLogs 测试 ====================

    @Test
    @DisplayName("获取日志列表 - 有租户ID时按租户查询")
    void getLogs_shouldReturnLogsByTenantId() {
        // 准备测试数据
        SystemLog log1 = new SystemLog();
        log1.setId(1L);
        SystemLog log2 = new SystemLog();
        log2.setId(2L);
        Page<SystemLog> logPage = new PageImpl<>(List.of(log1, log2));

        when(systemLogRepository.findByTenantId(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(logPage);

        // 执行测试
        PageResult<SystemLog> result = systemLogService.getLogs(0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());
        verify(systemLogRepository).findByTenantId(eq(TENANT_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("获取日志列表 - 无租户ID时查询全部")
    void getLogs_shouldReturnAllLogsWhenNoTenantId() {
        // 设置无租户ID
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(null);

        SystemLog log = new SystemLog();
        log.setId(1L);
        Page<SystemLog> logPage = new PageImpl<>(List.of(log));

        when(systemLogRepository.findAll(any(Pageable.class))).thenReturn(logPage);

        // 执行测试
        PageResult<SystemLog> result = systemLogService.getLogs(0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        verify(systemLogRepository).findAll(any(Pageable.class));
        verify(systemLogRepository, never()).findByTenantId(anyLong(), any(Pageable.class));
    }

    // ==================== getLogsByDateRange 测试 ====================

    @Test
    @DisplayName("按日期范围获取日志 - 有租户ID时按租户和日期范围查询")
    void getLogsByDateRange_shouldReturnLogsByTenantAndDateRange() {
        // 准备测试数据
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        SystemLog log = new SystemLog();
        log.setId(1L);
        Page<SystemLog> logPage = new PageImpl<>(List.of(log));

        when(systemLogRepository.findByTenantIdAndCreatedAtBetween(
                eq(TENANT_ID), eq(startTime), eq(endTime), any(Pageable.class)))
                .thenReturn(logPage);

        // 执行测试
        PageResult<SystemLog> result = systemLogService.getLogsByDateRange(startTime, endTime, 0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        verify(systemLogRepository).findByTenantIdAndCreatedAtBetween(
                eq(TENANT_ID), eq(startTime), eq(endTime), any(Pageable.class));
    }

    // ==================== getLogsByModule 测试 ====================

    @Test
    @DisplayName("按模块获取日志 - 有租户ID时按租户和模块查询")
    void getLogsByModule_shouldReturnLogsByTenantAndModule() {
        // 准备测试数据
        SystemLog log1 = new SystemLog();
        log1.setId(1L);
        log1.setModule("Agent");
        SystemLog log2 = new SystemLog();
        log2.setId(2L);
        log2.setModule("Agent");
        Page<SystemLog> logPage = new PageImpl<>(List.of(log1, log2));

        when(systemLogRepository.findByTenantIdAndModule(eq(TENANT_ID), eq("Agent"), any(Pageable.class)))
                .thenReturn(logPage);

        // 执行测试
        PageResult<SystemLog> result = systemLogService.getLogsByModule("Agent", 0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());
        verify(systemLogRepository).findByTenantIdAndModule(eq(TENANT_ID), eq("Agent"), any(Pageable.class));
    }

    @Test
    @DisplayName("按模块获取日志 - 无租户ID时查询全部")
    void getLogsByModule_shouldReturnAllWhenNoTenantId() {
        // 设置无租户ID
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(null);

        SystemLog log = new SystemLog();
        log.setId(1L);
        Page<SystemLog> logPage = new PageImpl<>(List.of(log));

        when(systemLogRepository.findAll(any(Pageable.class))).thenReturn(logPage);

        // 执行测试
        PageResult<SystemLog> result = systemLogService.getLogsByModule("Agent", 0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        verify(systemLogRepository).findAll(any(Pageable.class));
    }

    // ==================== getLogById 测试 ====================

    @Test
    @DisplayName("根据ID获取日志 - 成功返回日志")
    void getLogById_shouldReturnLog() {
        // 准备测试数据
        SystemLog log = new SystemLog();
        log.setId(LOG_ID);
        log.setModule("Agent");

        when(systemLogRepository.findById(LOG_ID)).thenReturn(java.util.Optional.of(log));

        // 执行测试
        SystemLog result = systemLogService.getLogById(LOG_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(LOG_ID, result.getId());
    }

    @Test
    @DisplayName("根据ID获取日志 - 日志不存在时返回null")
    void getLogById_shouldReturnNullWhenNotFound() {
        // 模拟日志不存在
        when(systemLogRepository.findById(LOG_ID)).thenReturn(java.util.Optional.empty());

        // 执行测试
        SystemLog result = systemLogService.getLogById(LOG_ID);

        // 验证结果
        assertNull(result);
    }

    // ==================== toJson 测试 ====================

    @Test
    @DisplayName("对象转JSON - 成功序列化")
    void toJson_shouldSerializeSuccessfully() throws Exception {
        // 准备测试数据
        Object obj = new Object() {
            @Override
            public String toString() {
                return "testObject";
            }
        };

        when(objectMapper.writeValueAsString(obj)).thenReturn("{\"key\":\"value\"}");

        // 执行测试
        String result = systemLogService.toJson(obj);

        // 验证结果
        assertEquals("{\"key\":\"value\"}", result);
        verify(objectMapper).writeValueAsString(obj);
    }

    @Test
    @DisplayName("对象转JSON - 序列化失败时返回toString结果")
    void toJson_shouldReturnToStringWhenSerializationFails() throws Exception {
        // 准备测试数据
        Object obj = "testString";

        when(objectMapper.writeValueAsString(obj))
                .thenThrow(com.fasterxml.jackson.core.JsonProcessingException.class);

        // 执行测试
        String result = systemLogService.toJson(obj);

        // 验证结果
        assertEquals("testString", result);
    }
}
