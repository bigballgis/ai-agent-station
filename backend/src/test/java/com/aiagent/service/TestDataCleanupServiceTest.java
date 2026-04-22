package com.aiagent.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TestDataCleanupService 单元测试
 * 测试测试数据的清理功能，包括按时间、按租户、按Agent清理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("测试数据清理服务测试")
class TestDataCleanupServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query resultQuery;

    @Mock
    private Query executionQuery;

    @InjectMocks
    private TestDataCleanupService testDataCleanupService;

    // ==================== cleanupTestData (按Agent) 测试 ====================

    @Test
    @DisplayName("按Agent清理测试数据 - 成功")
    void cleanupTestData_Success() {
        // 模拟测试结果查询
        when(entityManager.createNativeQuery(anyString())).thenReturn(resultQuery);
        when(resultQuery.setParameter(anyString(), any())).thenReturn(resultQuery);
        when(resultQuery.executeUpdate()).thenReturn(5);

        // 第一次调用返回测试结果删除数，第二次调用返回执行记录删除数
        // 由于方法内部创建了两个不同的Query，需要分别模拟
        when(entityManager.createNativeQuery(contains("agent_test_results")))
                .thenReturn(resultQuery);
        when(entityManager.createNativeQuery(contains("agent_test_executions")))
                .thenReturn(executionQuery);
        when(resultQuery.setParameter(anyString(), any())).thenReturn(resultQuery);
        when(executionQuery.setParameter(anyString(), any())).thenReturn(executionQuery);
        when(resultQuery.executeUpdate()).thenReturn(3);
        when(executionQuery.executeUpdate()).thenReturn(2);

        // 执行清理
        assertDoesNotThrow(() -> testDataCleanupService.cleanupTestData(100L, 1L));

        // 验证两次查询都被执行
        verify(resultQuery).executeUpdate();
        verify(executionQuery).executeUpdate();
    }

    @Test
    @DisplayName("按Agent清理测试数据 - 无数据可清理")
    void cleanupTestData_NoData() {
        when(entityManager.createNativeQuery(contains("agent_test_results")))
                .thenReturn(resultQuery);
        when(entityManager.createNativeQuery(contains("agent_test_executions")))
                .thenReturn(executionQuery);
        when(resultQuery.setParameter(anyString(), any())).thenReturn(resultQuery);
        when(executionQuery.setParameter(anyString(), any())).thenReturn(executionQuery);
        when(resultQuery.executeUpdate()).thenReturn(0);
        when(executionQuery.executeUpdate()).thenReturn(0);

        assertDoesNotThrow(() -> testDataCleanupService.cleanupTestData(100L, 1L));

        verify(resultQuery).executeUpdate();
        verify(executionQuery).executeUpdate();
    }

    // ==================== cleanupByAgent (按租户) 测试 ====================

    @Test
    @DisplayName("按租户清理测试数据 - 成功")
    void cleanupTenantTestData_Success() {
        when(entityManager.createNativeQuery(contains("agent_test_results")))
                .thenReturn(resultQuery);
        when(entityManager.createNativeQuery(contains("agent_test_executions")))
                .thenReturn(executionQuery);
        when(resultQuery.setParameter(anyString(), any())).thenReturn(resultQuery);
        when(executionQuery.setParameter(anyString(), any())).thenReturn(executionQuery);
        when(resultQuery.executeUpdate()).thenReturn(10);
        when(executionQuery.executeUpdate()).thenReturn(5);

        assertDoesNotThrow(() -> testDataCleanupService.cleanupTenantTestData(100L));

        verify(resultQuery).executeUpdate();
        verify(executionQuery).executeUpdate();
    }

    @Test
    @DisplayName("按租户清理测试数据 - 清理异常时不抛出")
    void cleanupTenantTestData_ExceptionHandled() {
        when(entityManager.createNativeQuery(anyString()))
                .thenThrow(new RuntimeException("数据库连接异常"));

        // 方法内部捕获了异常，不会向外抛出
        assertDoesNotThrow(() -> testDataCleanupService.cleanupTenantTestData(100L));
    }

    // ==================== cleanupByDateRange (定时清理过期数据) 测试 ====================

    @Test
    @DisplayName("清理过期测试数据 - 成功")
    void cleanupExpiredTestData_Success() {
        when(entityManager.createNativeQuery(contains("agent_test_results")))
                .thenReturn(resultQuery);
        when(entityManager.createNativeQuery(contains("agent_test_executions")))
                .thenReturn(executionQuery);
        when(resultQuery.setParameter(anyString(), any())).thenReturn(resultQuery);
        when(executionQuery.setParameter(anyString(), any())).thenReturn(executionQuery);
        when(resultQuery.executeUpdate()).thenReturn(20);
        when(executionQuery.executeUpdate()).thenReturn(15);

        assertDoesNotThrow(() -> testDataCleanupService.cleanupExpiredTestData());

        verify(resultQuery).executeUpdate();
        verify(executionQuery).executeUpdate();
    }

    @Test
    @DisplayName("清理过期测试数据 - 数据库异常时捕获处理")
    void cleanupExpiredTestData_ExceptionHandled() {
        when(entityManager.createNativeQuery(anyString()))
                .thenThrow(new RuntimeException("数据库异常"));

        // 方法内部捕获了异常，不会向外抛出
        assertDoesNotThrow(() -> testDataCleanupService.cleanupExpiredTestData());
    }

    // ==================== manualCleanup 测试 ====================

    @Test
    @DisplayName("手动触发清理 - 成功")
    void manualCleanup_Success() {
        when(entityManager.createNativeQuery(contains("agent_test_results")))
                .thenReturn(resultQuery);
        when(entityManager.createNativeQuery(contains("agent_test_executions")))
                .thenReturn(executionQuery);
        when(resultQuery.setParameter(anyString(), any())).thenReturn(resultQuery);
        when(executionQuery.setParameter(anyString(), any())).thenReturn(executionQuery);
        when(resultQuery.executeUpdate()).thenReturn(8);
        when(executionQuery.executeUpdate()).thenReturn(4);

        assertDoesNotThrow(() -> testDataCleanupService.manualCleanup());

        verify(resultQuery).executeUpdate();
        verify(executionQuery).executeUpdate();
    }

    @Test
    @DisplayName("手动触发清理 - 参数正确传递")
    void manualCleanup_CutoffDateParameter() {
        when(entityManager.createNativeQuery(anyString()))
                .thenReturn(resultQuery);
        when(resultQuery.setParameter(anyString(), any())).thenReturn(resultQuery);
        when(resultQuery.executeUpdate()).thenReturn(0);

        testDataCleanupService.manualCleanup();

        // 验证设置了cutoffDate参数
        verify(resultQuery, atLeastOnce()).setParameter(eq("cutoffDate"), any());
    }
}
