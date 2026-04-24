package com.aiagent.service;

import com.aiagent.config.DataRetentionConfig;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.entity.LoginLog;
import com.aiagent.entity.SystemLog;
import com.aiagent.repository.AgentTestResultRepository;
import com.aiagent.repository.LoginLogRepository;
import com.aiagent.repository.SystemLogRepository;
import com.aiagent.repository.TenantRepository;
import com.aiagent.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyInt;
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

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SystemLogService systemLogService;

    @Mock
    private DataRetentionConfig retentionConfig;

    @InjectMocks
    private DataRetentionPolicyService policyService;

    @BeforeEach
    void setUp() {
        // Configure retentionConfig defaults
        when(retentionConfig.getLogsDays()).thenReturn(90);
        when(retentionConfig.getTestResultsDays()).thenReturn(180);
        when(retentionConfig.getAuditLogsDays()).thenReturn(365);
        when(retentionConfig.getLoginLogsDays()).thenReturn(180);
    }

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

        // 验证 config setter 被调用
        verify(retentionConfig).setLogsDays(30);
        verify(retentionConfig).setTestResultsDays(60);
        verify(retentionConfig).setAuditLogsDays(90);
        verify(retentionConfig).setLoginLogsDays(120);
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

        // 验证结果 - 只更新了logs
        verify(retentionConfig).setLogsDays(15);
        verify(retentionConfig, never()).setTestResultsDays(anyInt());
    }

    @Test
    @DisplayName("更新保留策略 - 空策略时不做修改")
    void updateRetentionPolicy_shouldNotChangeWhenEmpty() {
        // 准备测试数据 - 空策略
        Map<String, Object> emptyPolicy = Map.of();

        // 执行测试
        Map<String, Object> result = policyService.updateRetentionPolicy(emptyPolicy);

        // 验证结果 - 所有setter不应被调用
        verify(retentionConfig, never()).setLogsDays(anyInt());
        verify(retentionConfig, never()).setTestResultsDays(anyInt());
        verify(retentionConfig, never()).setAuditLogsDays(anyInt());
        verify(retentionConfig, never()).setLoginLogsDays(anyInt());
    }

    // ==================== executeRetentionCleanup 测试 ====================

    @Test
    @DisplayName("执行保留策略清理 - 成功清理所有过期数据")
    void executeRetentionCleanup_shouldCleanupAllExpiredData() {
        // 准备测试数据
        when(systemLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(5);
        when(testResultRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(3);
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(2);

        // 执行测试
        policyService.executeRetentionCleanup();

        // 验证结果
        verify(systemLogRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
        verify(testResultRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
        verify(loginLogRepository).findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(userSessionRepository).deleteInactiveSessionsBefore(any(LocalDateTime.class));
        // 验证审计日志被记录
        verify(systemLogService).saveLog(any(SystemLog.class));
    }

    @Test
    @DisplayName("执行保留策略清理 - 清理过期测试结果")
    void executeRetentionCleanup_shouldCleanupExpiredTestResults() {
        // 准备测试数据
        when(systemLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0);
        when(testResultRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(10);
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(0);

        // 执行测试
        policyService.executeRetentionCleanup();

        // 验证结果
        verify(testResultRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("执行保留策略清理 - 清理过期登录日志")
    void executeRetentionCleanup_shouldCleanupExpiredLoginLogs() {
        // 准备测试数据
        LoginLog expiredLog = new LoginLog();
        expiredLog.setId(1L);

        when(systemLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0);
        when(testResultRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0);
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
        when(systemLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0);
        when(testResultRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0);
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
        when(systemLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("数据库异常"));
        when(testResultRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0);
        when(loginLogRepository.findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userSessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(0);

        // 执行测试 - 不应抛出异常
        assertDoesNotThrow(() -> policyService.executeRetentionCleanup());

        // 验证结果 - 其他清理仍然执行
        verify(testResultRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
        verify(loginLogRepository).findByLoginTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(userSessionRepository).deleteInactiveSessionsBefore(any(LocalDateTime.class));
    }

    // ==================== executeRetentionCleanupForTenant 测试 ====================

    @Test
    @DisplayName("执行租户保留策略清理 - 成功按租户清理")
    void executeRetentionCleanupForTenant_shouldCleanupByTenant() {
        Long tenantId = 100L;

        when(systemLogRepository.deleteByTenantIdAndCreatedAtBefore(eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(3);
        when(testResultRepository.deleteByTenantIdAndCreatedAtBefore(eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(2);

        // 执行测试
        long result = policyService.executeRetentionCleanupForTenant(tenantId);

        // 验证结果
        assertEquals(5, result);
        verify(systemLogRepository).deleteByTenantIdAndCreatedAtBefore(eq(tenantId), any(LocalDateTime.class));
        verify(testResultRepository).deleteByTenantIdAndCreatedAtBefore(eq(tenantId), any(LocalDateTime.class));
        // 验证审计日志被记录
        verify(systemLogService).saveLog(any(SystemLog.class));
    }
}
