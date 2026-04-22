package com.aiagent.service;

import com.aiagent.entity.AgentTestResult;
import com.aiagent.entity.LoginLog;
import com.aiagent.entity.SystemLog;
import com.aiagent.repository.AgentTestResultRepository;
import com.aiagent.repository.LoginLogRepository;
import com.aiagent.repository.SystemLogRepository;
import com.aiagent.repository.UserSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DataRetentionPolicyService 单元测试
 * 测试数据保留策略服务的核心方法：获取保留策略、更新保留策略、执行保留清理等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("数据保留策略服务测试")
class DataRetentionPolicyServiceTest {

    @Mock
    private SystemLogRepository systemLogRepository;

    @Mock
    private AgentTestResultRepository testResultRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private DataRetentionPolicyService policyService;

    // ==================== getRetentionPolicy 测试 ====================

    @Test
    @DisplayName("获取保留策略 - 成功返回默认策略配置")
    void getRetentionPolicy_shouldReturnDefaultPolicy() {
        // 执行测试
        Map<String, Object> result = policyService.getRetentionPolicy();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("logs"));
        assertTrue(result.containsKey("testResults"));
        assertTrue(result.containsKey("auditLogs"));
        assertTrue(result.containsKey("loginLogs"));

        // 验证默认值
        Map<String, Object> logsPolicy = (Map<String, Object>) result.get("logs");
        assertEquals(90, logsPolicy.get("retentionDays"));

        Map<String, Object> testResultsPolicy = (Map<String, Object>) result.get("testResults");
        assertEquals(180, testResultsPolicy.get("retentionDays"));

        Map<String, Object> auditLogsPolicy = (Map<String, Object>) result.get("auditLogs");
        assertEquals(365, auditLogsPolicy.get("retentionDays"));

        Map<String, Object> loginLogsPolicy = (Map<String, Object>) result.get("loginLogs");
        assertEquals(180, loginLogsPolicy.get("retentionDays"));
    }

    // ==================== updateRetentionPolicy 测试 ====================

    @Test
    @DisplayName("更新保留策略 - 成功更新所有策略项")
    void updateRetentionPolicy_shouldUpdateAllPolicies() {
        // 准备测试数据
        Map<String, Object> newPolicy = Map.of(
                "logsRetentionDays", 30,
                "testResultsRetentionDays", 60,
                "auditLogsRetentionDays", 90,
                "loginLogsRetentionDays", 120
        );

        // 执行测试
        Map<String, Object> result = policyService.updateRetentionPolicy(newPolicy);

        // 验证结果
        assertNotNull(result);

        Map<String, Object> logsPolicy = (Map<String, Object>) result.get("logs");
        assertEquals(30, logsPolicy.get("retentionDays"));

        Map<String, Object> testResultsPolicy = (Map<String, Object>) result.get("testResults");
        assertEquals(60, testResultsPolicy.get("retentionDays"));

        Map<String, Object> auditLogsPolicy = (Map<String, Object>) result.get("auditLogs");
        assertEquals(90, auditLogsPolicy.get("retentionDays"));

        Map<String, Object> loginLogsPolicy = (Map<String, Object>) result.get("loginLogs");
        assertEquals(120, loginLogsPolicy.get("retentionDays"));
    }

    @Test
    @DisplayName("更新保留策略 - 部分更新策略项")
    void updateRetentionPolicy_shouldUpdatePartialPolicies() {
        // 准备测试数据 - 只更新部分策略
        Map<String, Object> partialPolicy = Map.of(
                "logsRetentionDays", 15
        );

        // 执行测试
        Map<String, Object> result = policyService.updateRetentionPolicy(partialPolicy);

        // 验证结果 - 只更新了logs，其他保持默认值
        Map<String, Object> logsPolicy = (Map<String, Object>) result.get("logs");
        assertEquals(15, logsPolicy.get("retentionDays"));

        Map<String, Object> testResultsPolicy = (Map<String, Object>) result.get("testResults");
        assertEquals(180, testResultsPolicy.get("retentionDays")); // 保持默认值
    }

    @Test
    @DisplayName("更新保留策略 - 空策略时不做修改")
    void updateRetentionPolicy_shouldNotChangeWhenEmpty() {
        // 准备测试数据 - 空策略
        Map<String, Object> emptyPolicy = Map.of();

        // 执行测试
        Map<String, Object> result = policyService.updateRetentionPolicy(emptyPolicy);

        // 验证结果 - 所有值保持默认
        Map<String, Object> logsPolicy = (Map<String, Object>) result.get("logs");
        assertEquals(90, logsPolicy.get("retentionDays"));
    }

    // ==================== executeRetentionCleanup 测试 ====================

    @Test
    @DisplayName("执行保留策略清理 - 成功清理所有过期数据")
    void executeRetentionCleanup_shouldCleanupAllExpiredData() {
        // 准备测试数据 - 模拟空数据
        SystemLog expiredLog = new SystemLog();
        expiredLog.setId(1L);
        expiredLog.setCreatedAt(LocalDateTime.now().minusDays(100)); // 超过90天

        when(systemLogRepository.findAll()).thenReturn(List.of(expiredLog));
        when(testResultRepository.findAll()).thenReturn(List.of());
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(0);

        // 执行测试
        policyService.executeRetentionCleanup();

        // 验证结果
        verify(systemLogRepository).findAll();
        verify(systemLogRepository).delete(expiredLog);
        verify(testResultRepository).findAll();
        verify(loginLogRepository).findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(userSessionRepository).deleteInactiveSessionsBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("执行保留策略清理 - 清理过期测试结果")
    void executeRetentionCleanup_shouldCleanupExpiredTestResults() {
        // 准备测试数据
        AgentTestResult expiredResult = new AgentTestResult();
        expiredResult.setId(1L);
        expiredResult.setCreatedAt(LocalDateTime.now().minusDays(200)); // 超过180天

        when(systemLogRepository.findAll()).thenReturn(List.of());
        when(testResultRepository.findAll()).thenReturn(List.of(expiredResult));
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(0);

        // 执行测试
        policyService.executeRetentionCleanup();

        // 验证结果
        verify(testResultRepository).findAll();
        verify(testResultRepository).delete(expiredResult);
    }

    @Test
    @DisplayName("执行保留策略清理 - 清理过期登录日志")
    void executeRetentionCleanup_shouldCleanupExpiredLoginLogs() {
        // 准备测试数据
        LoginLog expiredLog = new LoginLog();
        expiredLog.setId(1L);

        when(systemLogRepository.findAll()).thenReturn(List.of());
        when(testResultRepository.findAll()).thenReturn(List.of());
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredLog));
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(0);

        // 执行测试
        policyService.executeRetentionCleanup();

        // 验证结果
        verify(loginLogRepository).findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(loginLogRepository).deleteAll(List.of(expiredLog));
    }

    @Test
    @DisplayName("执行保留策略清理 - 清理过期会话")
    void executeRetentionCleanup_shouldCleanupExpiredSessions() {
        // 准备测试数据
        when(systemLogRepository.findAll()).thenReturn(List.of());
        when(testResultRepository.findAll()).thenReturn(List.of());
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(5);

        // 执行测试
        policyService.executeRetentionCleanup();

        // 验证结果
        verify(userSessionRepository).deleteInactiveSessionsBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("执行保留策略清理 - 系统日志清理异常时继续执行其他清理")
    void executeRetentionCleanup_shouldContinueWhenSystemLogCleanupFails() {
        // 模拟系统日志清理异常
        when(systemLogRepository.findAll()).thenThrow(new RuntimeException("数据库异常"));
        when(testResultRepository.findAll()).thenReturn(List.of());
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(0);

        // 执行测试 - 不应抛出异常
        assertDoesNotThrow(() -> policyService.executeRetentionCleanup());

        // 验证结果 - 其他清理仍然执行
        verify(testResultRepository).findAll();
        verify(loginLogRepository).findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(userSessionRepository).deleteInactiveSessionsBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("执行保留策略清理 - 未过期的数据不被删除")
    void executeRetentionCleanup_shouldNotDeleteNonExpiredData() {
        // 准备测试数据 - 未过期的日志
        SystemLog recentLog = new SystemLog();
        recentLog.setId(1L);
        recentLog.setCreatedAt(LocalDateTime.now().minusDays(30)); // 未超过90天

        when(systemLogRepository.findAll()).thenReturn(List.of(recentLog));
        when(testResultRepository.findAll()).thenReturn(List.of());
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(0);

        // 执行测试
        policyService.executeRetentionCleanup();

        // 验证结果 - 未过期的日志不应被删除
        verify(systemLogRepository, never()).delete(any(SystemLog.class));
    }
}
