package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.dto.TenantQuotaUpdateDTO;
import com.aiagent.entity.Tenant;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.TenantRepository;
import com.aiagent.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QuotaService 单元测试
 * 覆盖配额检查、计数器递增/递减、配额查询及更新等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("配额服务测试")
class QuotaServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private QuotaService quotaService;

    private Tenant testTenant;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("测试租户");
        testTenant.setIsActive(true);
        testTenant.setMaxAgents(100);
        testTenant.setUsedAgents(30);
        testTenant.setMaxWorkflows(50);
        testTenant.setUsedWorkflows(10);
        testTenant.setMaxApiCallsPerDay(10000L);
        testTenant.setUsedApiCallsToday(5000L);
        testTenant.setMaxTokensPerDay(1000000L);
        testTenant.setUsedTokensToday(200000L);
        testTenant.setMaxMcpCallsPerDay(5000L);
        testTenant.setMaxStorageMb(1024L);

        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentTenantId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ==================== checkAgentQuota ====================

    @Test
    @DisplayName("检查Agent配额 - 未超限不抛异常")
    void checkAgentQuota_underLimit_shouldNotThrow() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertDoesNotThrow(() -> quotaService.checkAgentQuota());
    }

    @Test
    @DisplayName("检查Agent配额 - 达到上限抛出异常")
    void checkAgentQuota_atLimit_shouldThrowException() {
        testTenant.setUsedAgents(100);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> quotaService.checkAgentQuota());

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("Agent"));
        assertTrue(exception.getMessage().contains("100"));
    }

    @Test
    @DisplayName("检查Agent配额 - maxAgents 为 null 时不做限制")
    void checkAgentQuota_nullMaxAgents_shouldNotThrow() {
        testTenant.setMaxAgents(null);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertDoesNotThrow(() -> quotaService.checkAgentQuota());
    }

    // ==================== checkWorkflowQuota ====================

    @Test
    @DisplayName("检查工作流配额 - 未超限不抛异常")
    void checkWorkflowQuota_underLimit_shouldNotThrow() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertDoesNotThrow(() -> quotaService.checkWorkflowQuota());
    }

    @Test
    @DisplayName("检查工作流配额 - 达到上限抛出异常")
    void checkWorkflowQuota_atLimit_shouldThrowException() {
        testTenant.setUsedWorkflows(50);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> quotaService.checkWorkflowQuota());

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("工作流"));
    }

    // ==================== checkStorageQuota ====================

    @Test
    @DisplayName("检查存储配额 - 文件大小未超限不抛异常")
    void checkStorageQuota_underLimit_shouldNotThrow() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        // maxStorageMb=1024, 单文件限制 = 1024*1024*1024/10 = 107374182 bytes (~102MB)
        assertDoesNotThrow(() -> quotaService.checkStorageQuota(50 * 1024 * 1024L));
    }

    @Test
    @DisplayName("检查存储配额 - 文件大小超过限制抛出异常")
    void checkStorageQuota_overLimit_shouldThrowException() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        // 单文件限制 = 1024*1024*1024/10 = 107374182 bytes
        BusinessException exception = assertThrows(BusinessException.class,
                () -> quotaService.checkStorageQuota(200 * 1024 * 1024L));

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("单文件大小超过限制"));
    }

    @Test
    @DisplayName("检查存储配额 - maxStorageMb 为 null 时不做限制")
    void checkStorageQuota_nullMaxStorage_shouldNotThrow() {
        testTenant.setMaxStorageMb(null);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertDoesNotThrow(() -> quotaService.checkStorageQuota(Long.MAX_VALUE));
    }

    // ==================== incrementAgentCount ====================

    @Test
    @DisplayName("递增Agent计数 - usedAgents 加 1")
    void incrementAgentCount_shouldIncrement() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        quotaService.incrementAgentCount();

        assertEquals(31, testTenant.getUsedAgents());
        verify(tenantRepository).save(testTenant);
    }

    // ==================== decrementAgentCount ====================

    @Test
    @DisplayName("递减Agent计数 - usedAgents 减 1")
    void decrementAgentCount_shouldDecrement() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        quotaService.decrementAgentCount();

        assertEquals(29, testTenant.getUsedAgents());
        verify(tenantRepository).save(testTenant);
    }

    @Test
    @DisplayName("递减Agent计数 - usedAgents 为 0 时不低于 0")
    void decrementAgentCount_atZero_shouldStayZero() {
        testTenant.setUsedAgents(0);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        quotaService.decrementAgentCount();

        assertEquals(0, testTenant.getUsedAgents());
    }

    // ==================== getTenantQuotaDetails ====================

    @Test
    @DisplayName("获取配额详细明细 - 成功返回完整配额信息")
    void getTenantQuotaDetails_shouldReturnFullDetails() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        Map<String, Object> result = quotaService.getTenantQuotaDetails(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("tenantId"));
        assertEquals("测试租户", result.get("tenantName"));
        assertTrue(result.containsKey("agents"));
        assertTrue(result.containsKey("workflows"));
        assertTrue(result.containsKey("apiCalls"));
        assertTrue(result.containsKey("tokens"));
        assertTrue(result.containsKey("mcpCalls"));
        assertTrue(result.containsKey("storage"));

        @SuppressWarnings("unchecked")
        Map<String, Object> agentQuota = (Map<String, Object>) result.get("agents");
        assertEquals(30, agentQuota.get("used"));
        assertEquals(100, agentQuota.get("limit"));
        assertEquals(70, agentQuota.get("remaining"));
        assertEquals(30.0, agentQuota.get("usagePercent"));
    }

    @Test
    @DisplayName("获取配额详细明细 - 租户不存在抛出异常")
    void getTenantQuotaDetails_tenantNotFound_shouldThrow() {
        when(tenantRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class,
                () -> quotaService.getTenantQuotaDetails(999L));
    }

    @Test
    @DisplayName("获取配额详细明细 - 配额使用率100%边界测试")
    void getTenantQuotaDetails_fullUsage_shouldReturnZeroRemaining() {
        testTenant.setUsedAgents(100);
        testTenant.setUsedApiCallsToday(10000L);
        testTenant.setUsedTokensToday(1000000L);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        Map<String, Object> result = quotaService.getTenantQuotaDetails(1L);

        @SuppressWarnings("unchecked")
        Map<String, Object> agentQuota = (Map<String, Object>) result.get("agents");
        assertEquals(0, agentQuota.get("remaining"));
        assertEquals(100.0, agentQuota.get("usagePercent"));
    }

    // ==================== getTenantQuota ====================

    @Test
    @DisplayName("获取租户配额摘要 - 成功")
    void getTenantQuota_shouldReturnSummary() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        Map<String, Object> result = quotaService.getTenantQuota(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("tenantId"));
        assertEquals("测试租户", result.get("tenantName"));
        assertEquals(30, result.get("agentCount"));
        assertEquals(100, result.get("agentLimit"));
    }

    // ==================== updateTenantQuota ====================

    @Test
    @DisplayName("更新租户配额 - 成功更新并返回最新配额")
    void updateTenantQuota_shouldUpdateAndReturn() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantQuotaUpdateDTO dto = new TenantQuotaUpdateDTO();
        dto.setAgentLimit(200L);
        dto.setApiCallLimit(20000L);
        dto.setTokenLimit(2000000L);
        dto.setStorageLimit(2048L);

        Map<String, Object> result = quotaService.updateTenantQuota(1L, dto);

        assertNotNull(result);
        assertEquals(200, testTenant.getMaxAgents());
        assertEquals(20000L, testTenant.getMaxApiCallsPerDay());
        assertEquals(2000000L, testTenant.getMaxTokensPerDay());
        assertEquals(2048L, testTenant.getMaxStorageMb());
        verify(tenantRepository).save(testTenant);
    }

    @Test
    @DisplayName("更新租户配额 - 部分更新仅修改非 null 字段")
    void updateTenantQuota_partialUpdate_shouldOnlyUpdateNonNull() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantQuotaUpdateDTO dto = new TenantQuotaUpdateDTO();
        dto.setAgentLimit(200L);
        // 其他字段为 null

        quotaService.updateTenantQuota(1L, dto);

        assertEquals(200, testTenant.getMaxAgents());
        assertEquals(10000L, testTenant.getMaxApiCallsPerDay()); // 未变
        assertEquals(1000000L, testTenant.getMaxTokensPerDay()); // 未变
        assertEquals(1024L, testTenant.getMaxStorageMb()); // 未变
    }

    // ==================== checkApiCallQuota ====================

    @Test
    @DisplayName("检查API调用配额 - 未超限不抛异常")
    void checkApiCallQuota_underLimit_shouldNotThrow() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertDoesNotThrow(() -> quotaService.checkApiCallQuota());
    }

    @Test
    @DisplayName("检查API调用配额 - 达到上限抛出异常")
    void checkApiCallQuota_atLimit_shouldThrowException() {
        testTenant.setUsedApiCallsToday(10000L);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertThrows(BusinessException.class, () -> quotaService.checkApiCallQuota());
    }

    // ==================== checkTokenQuota ====================

    @Test
    @DisplayName("检查Token配额 - 未超限不抛异常")
    void checkTokenQuota_underLimit_shouldNotThrow() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertDoesNotThrow(() -> quotaService.checkTokenQuota());
    }

    @Test
    @DisplayName("检查Token配额 - 达到上限抛出异常")
    void checkTokenQuota_atLimit_shouldThrowException() {
        testTenant.setUsedTokensToday(1000000L);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertThrows(BusinessException.class, () -> quotaService.checkTokenQuota());
    }

    // ==================== incrementTokenUsage ====================

    @Test
    @DisplayName("递增Token用量 - 按指定数量递增")
    void incrementTokenUsage_shouldAddTokens() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        quotaService.incrementTokenUsage(5000L);

        assertEquals(205000L, testTenant.getUsedTokensToday());
        verify(tenantRepository).save(testTenant);
    }
}
