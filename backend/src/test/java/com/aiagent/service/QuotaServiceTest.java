package com.aiagent.service;

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
 * 测试租户配额查询、更新等功能
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
        // 初始化测试租户数据
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("测试租户");
        testTenant.setIsActive(true);
        testTenant.setMaxAgents(100);
        testTenant.setUsedAgents(30);
        testTenant.setMaxApiCallsPerDay(10000L);
        testTenant.setUsedApiCallsToday(5000L);
        testTenant.setMaxTokensPerDay(1000000L);
        testTenant.setUsedTokensToday(200000L);
        testTenant.setMaxMcpCallsPerDay(5000L);
        testTenant.setMaxStorageMb(1024L);

        // 模拟 SecurityUtils
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentTenantId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("获取租户配额摘要 - 成功")
    void testGetTenantQuota() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        Map<String, Object> result = quotaService.getTenantQuota(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("tenantId"));
        assertEquals("测试租户", result.get("tenantName"));
        assertEquals(30, result.get("agentCount"));
        assertEquals(100, result.get("agentLimit"));
        assertEquals(5000L, result.get("apiCallCount"));
        assertEquals(10000L, result.get("apiCallLimit"));
        assertEquals(200000L, result.get("tokenUsage"));
        assertEquals(1000000L, result.get("tokenLimit"));
    }

    @Test
    @DisplayName("获取租户配额摘要 - 租户不存在抛出异常")
    void testGetTenantQuota_TenantNotFound() {
        when(tenantRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> quotaService.getTenantQuota(999L));
    }

    @Test
    @DisplayName("更新租户配额 - 成功")
    void testUpdateTenantQuota() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> params = Map.of(
                "maxAgents", 200,
                "maxApiCallsPerDay", 20000,
                "maxTokensPerDay", 2000000
        );

        Map<String, Object> result = quotaService.updateTenantQuota(1L, params);

        assertNotNull(result);
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("获取租户配额详细明细 - 成功")
    void testGetTenantQuotaDetails() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        Map<String, Object> result = quotaService.getTenantQuotaDetails(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("tenantId"));
        assertTrue(result.containsKey("agents"));
        assertTrue(result.containsKey("apiCalls"));
        assertTrue(result.containsKey("tokens"));
        assertTrue(result.containsKey("mcpCalls"));
        assertTrue(result.containsKey("storage"));

        // 验证 Agent 配额计算
        // quotaService 返回 Map<String, Object>，从结果中取值需要强制转换
        @SuppressWarnings("unchecked")
        Map<String, Object> agentQuota = (Map<String, Object>) result.get("agents");
        assertEquals(30, agentQuota.get("used"));
        assertEquals(100, agentQuota.get("limit"));
        assertEquals(70, agentQuota.get("remaining"));
        assertEquals(30.0, agentQuota.get("usagePercent"));
    }

    @Test
    @DisplayName("获取租户配额详细明细 - 配额使用率100%边界测试")
    void testGetTenantQuotaDetails_FullUsage() {
        testTenant.setUsedAgents(100);
        testTenant.setUsedApiCallsToday(10000L);
        testTenant.setUsedTokensToday(1000000L);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        Map<String, Object> result = quotaService.getTenantQuotaDetails(1L);

        // quotaService 返回 Map<String, Object>，从结果中取值需要强制转换
        @SuppressWarnings("unchecked")
        Map<String, Object> agentQuota = (Map<String, Object>) result.get("agents");
        assertEquals(0, agentQuota.get("remaining"));
        assertEquals(100.0, agentQuota.get("usagePercent"));
    }

    @Test
    @DisplayName("检查Agent配额 - 未超限不抛异常")
    void testCheckAgentQuota_WithinLimit() {
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        // usedAgents=30, maxAgents=100, 未超限
        assertDoesNotThrow(() -> quotaService.checkAgentQuota());
    }

    @Test
    @DisplayName("检查Agent配额 - 超限抛出异常")
    void testCheckAgentQuota_Exceeded() {
        testTenant.setUsedAgents(100);
        when(tenantRepository.findById(1L)).thenReturn(java.util.Optional.of(testTenant));

        assertThrows(BusinessException.class, () -> quotaService.checkAgentQuota());
    }
}
