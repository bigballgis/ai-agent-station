package com.aiagent.service;

import com.aiagent.entity.*;
import com.aiagent.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserDataService 单元测试
 * 测试用户数据服务的核心方法：导出用户数据、匿名化用户、删除用户数据等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户数据服务测试")
class UserDataServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentTestResultRepository testResultRepository;

    @Mock
    private AgentTestExecutionRepository testExecutionRepository;

    @Mock
    private AgentTestCaseRepository testCaseRepository;

    @Mock
    private SystemLogRepository systemLogRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private DeploymentHistoryRepository deploymentHistoryRepository;

    @Mock
    private AgentApprovalRepository agentApprovalRepository;

    @InjectMocks
    private UserDataService userDataService;

    private static final Long USER_ID = 1L;
    private static final Long TENANT_ID = 10L;

    // ==================== exportUserData 测试 ====================

    @Test
    @DisplayName("导出用户数据 - 成功导出包含所有数据的Map")
    void exportUserData_shouldExportAllData() {
        // 准备测试数据
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
        user.setTenantId(TENANT_ID);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Agent agent = new Agent();
        agent.setId(1L);
        agent.setCreatedBy(USER_ID);
        agent.setTenantId(TENANT_ID);
        agent.setCreatedAt(LocalDateTime.now());

        LoginLog loginLog = new LoginLog();
        loginLog.setId(1L);
        loginLog.setUserId(USER_ID);
        loginLog.setUsername("testuser");

        UserSession session = new UserSession();
        session.setId(1L);
        session.setUserId(USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(agentRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(agent));
        when(testCaseRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());
        when(testExecutionRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());
        when(testResultRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());
        when(systemLogRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());
        when(loginLogRepository.findByUserIdOrderByLoginTimeDesc(USER_ID)).thenReturn(List.of(loginLog));
        when(userSessionRepository.findByUserId(USER_ID)).thenReturn(List.of(session));

        // 执行测试
        Map<String, Object> result = userDataService.exportUserData(USER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(USER_ID, result.get("userId"));
        assertNotNull(result.get("exportTime"));
        assertNotNull(result.get("userInfo"));
        assertNotNull(result.get("agents"));
        assertNotNull(result.get("loginLogs"));
        assertNotNull(result.get("sessions"));
        verify(userRepository, atLeastOnce()).findById(USER_ID);
    }

    @Test
    @DisplayName("导出用户数据 - 用户不存在时返回基本结构")
    void exportUserData_shouldReturnBasicStructureWhenUserNotFound() {
        // 模拟用户不存在
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(loginLogRepository.findByUserIdOrderByLoginTimeDesc(USER_ID)).thenReturn(List.of());
        when(userSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        // 执行测试
        Map<String, Object> result = userDataService.exportUserData(USER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(USER_ID, result.get("userId"));
        assertNull(result.get("userInfo"));
    }

    @Test
    @DisplayName("导出用户数据 - 用户无租户ID时不查询租户相关数据")
    void exportUserData_shouldSkipTenantDataWhenNoTenantId() {
        // 准备测试数据 - 用户无租户ID
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        user.setTenantId(null);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(loginLogRepository.findByUserIdOrderByLoginTimeDesc(USER_ID)).thenReturn(List.of());
        when(userSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        // 执行测试
        Map<String, Object> result = userDataService.exportUserData(USER_ID);

        // 验证结果
        assertNotNull(result);
        assertNull(result.get("agents"));
        verify(agentRepository, never()).findByTenantId(any());
    }

    // ==================== deleteUserData 测试 ====================

    @Test
    @DisplayName("删除用户数据 - 成功调用匿名化方法")
    void deleteUserData_shouldCallAnonymizeUser() {
        // 准备测试数据
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setTenantId(TENANT_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(loginLogRepository.findByUserIdOrderByLoginTimeDesc(USER_ID)).thenReturn(List.of());
        when(systemLogRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        // 执行测试
        userDataService.deleteUserData(USER_ID);

        // 验证结果 - 用户被匿名化
        verify(userRepository).save(argThat(savedUser -> {
            assertFalse(savedUser.getIsActive());
            assertNull(savedUser.getPhone());
            assertTrue(savedUser.getUsername().startsWith("anonymized_" + USER_ID + "_"));
            assertTrue(savedUser.getEmail().startsWith("anonymized_" + USER_ID + "_"));
            return true;
        }));
    }

    // ==================== anonymizeUser 测试 ====================

    @Test
    @DisplayName("匿名化用户 - 成功匿名化用户基本信息")
    void anonymizeUser_shouldAnonymizeUserInfo() {
        // 准备测试数据
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("original_user");
        user.setEmail("original@example.com");
        user.setPhone("13800138000");
        user.setIsActive(true);
        user.setTenantId(TENANT_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(loginLogRepository.findByUserIdOrderByLoginTimeDesc(USER_ID)).thenReturn(List.of());
        when(systemLogRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        // 执行测试
        userDataService.anonymizeUser(USER_ID);

        // 验证结果
        assertFalse(user.getIsActive());
        assertNull(user.getPhone());
        assertTrue(user.getUsername().startsWith("anonymized_"));
        assertTrue(user.getEmail().startsWith("anonymized_"));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("匿名化用户 - 成功清理会话信息")
    void anonymizeUser_shouldCleanSessions() {
        // 准备测试数据
        User user = new User();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);

        UserSession session = new UserSession();
        session.setId(1L);
        session.setUserId(USER_ID);
        session.setIpAddress("192.168.1.1");
        session.setUserAgent("Chrome");
        session.setBrowser("Chrome");
        session.setOs("Windows");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userSessionRepository.findByUserId(USER_ID)).thenReturn(List.of(session));
        when(loginLogRepository.findByUserIdOrderByLoginTimeDesc(USER_ID)).thenReturn(List.of());
        when(systemLogRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        // 执行测试
        userDataService.anonymizeUser(USER_ID);

        // 验证结果
        assertEquals("0.0.0.0", session.getIpAddress());
        assertEquals("Anonymized", session.getUserAgent());
        assertEquals("Unknown", session.getBrowser());
        assertEquals("Unknown", session.getOs());
        verify(userSessionRepository).save(session);
    }

    @Test
    @DisplayName("匿名化用户 - 成功清理登录日志IP")
    void anonymizeUser_shouldCleanLoginLogs() {
        // 准备测试数据
        User user = new User();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);

        LoginLog loginLog = new LoginLog();
        loginLog.setId(1L);
        loginLog.setUserId(USER_ID);
        loginLog.setIp("192.168.1.1");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(loginLogRepository.findByUserIdOrderByLoginTimeDesc(USER_ID)).thenReturn(List.of(loginLog));
        when(systemLogRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        // 执行测试
        userDataService.anonymizeUser(USER_ID);

        // 验证结果
        assertEquals("0.0.0.0", loginLog.getIp());
        verify(loginLogRepository).save(loginLog);
    }

    @Test
    @DisplayName("匿名化用户 - 用户不存在时不抛出异常")
    void anonymizeUser_shouldNotThrowWhenUserNotFound() {
        // 模拟用户不存在
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(loginLogRepository.findByUserIdOrderByLoginTimeDesc(USER_ID)).thenReturn(List.of());
        when(userSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        // 执行测试 - 不应抛出异常
        assertDoesNotThrow(() -> userDataService.anonymizeUser(USER_ID));
        verify(userRepository, never()).save(any());
    }

    // ==================== getDataRetentionReport 测试 ====================

    @Test
    @DisplayName("获取数据保留报告 - 成功返回报告")
    void getDataRetentionReport_shouldReturnReport() {
        // 模拟各仓库返回计数
        when(systemLogRepository.count()).thenReturn(100L);
        when(testResultRepository.count()).thenReturn(50L);
        when(loginLogRepository.count()).thenReturn(200L);
        when(userRepository.count()).thenReturn(30L);
        when(userSessionRepository.count()).thenReturn(80L);

        // 执行测试
        Map<String, Object> result = userDataService.getDataRetentionReport();

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.get("reportTime"));
        assertNotNull(result.get("systemLogs"));
        assertNotNull(result.get("testResults"));
        assertNotNull(result.get("loginLogs"));
        assertNotNull(result.get("users"));
        assertNotNull(result.get("sessions"));
        verify(systemLogRepository).count();
        verify(testResultRepository).count();
        verify(loginLogRepository).count();
        verify(userRepository).count();
        verify(userSessionRepository).count();
    }
}
