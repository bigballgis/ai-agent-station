package com.aiagent.service;

import com.aiagent.entity.AgentTestCase;
import com.aiagent.repository.AgentTestCaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AgentTestCaseService 单元测试
 * 测试测试用例服务的核心方法：创建用例、更新用例、删除用例、查询用例等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent测试用例服务测试")
class AgentTestCaseServiceTest {

    @Mock
    private AgentTestCaseRepository testCaseRepository;

    @InjectMocks
    private AgentTestCaseService testCaseService;

    private static final Long TEST_CASE_ID = 1L;
    private static final Long AGENT_ID = 100L;
    private static final Long TENANT_ID = 1L;

    // ==================== createTestCase 测试 ====================

    @Test
    @DisplayName("创建测试用例 - 成功创建并返回测试用例")
    void createTestCase_shouldCreateSuccessfully() {
        // 准备测试数据
        AgentTestCase testCase = new AgentTestCase();
        testCase.setTestName("登录测试");
        testCase.setTestCode("TC_LOGIN_001");
        testCase.setTestType("unit");
        testCase.setInputParams("{\"username\":\"admin\"}");
        testCase.setExpectedOutput("{\"status\":\"success\"}");
        testCase.setStatus(1);

        when(testCaseRepository.save(any(AgentTestCase.class))).thenAnswer(inv -> {
            AgentTestCase saved = inv.getArgument(0);
            saved.setId(TEST_CASE_ID);
            return saved;
        });

        // 执行测试
        AgentTestCase result = testCaseService.createTestCase(testCase);

        // 验证结果
        assertNotNull(result);
        assertEquals(TEST_CASE_ID, result.getId());
        assertEquals("登录测试", result.getTestName());
        verify(testCaseRepository).save(testCase);
    }

    // ==================== getTestCaseById 测试 ====================

    @Test
    @DisplayName("根据ID获取测试用例 - 成功返回测试用例")
    void getTestCaseById_shouldReturnTestCase() {
        // 准备测试数据
        AgentTestCase testCase = new AgentTestCase();
        testCase.setId(TEST_CASE_ID);
        testCase.setTestName("登录测试");

        when(testCaseRepository.findById(TEST_CASE_ID)).thenReturn(Optional.of(testCase));

        // 执行测试
        Optional<AgentTestCase> result = testCaseService.getTestCaseById(TEST_CASE_ID);

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(TEST_CASE_ID, result.get().getId());
        assertEquals("登录测试", result.get().getTestName());
    }

    @Test
    @DisplayName("根据ID获取测试用例 - 用例不存在时返回空Optional")
    void getTestCaseById_shouldReturnEmptyWhenNotFound() {
        // 模拟用例不存在
        when(testCaseRepository.findById(TEST_CASE_ID)).thenReturn(Optional.empty());

        // 执行测试
        Optional<AgentTestCase> result = testCaseService.getTestCaseById(TEST_CASE_ID);

        // 验证结果
        assertFalse(result.isPresent());
    }

    // ==================== getTestCasesByTenantId 测试 ====================

    @Test
    @DisplayName("根据租户ID获取测试用例列表 - 成功返回列表")
    void getTestCasesByTenantId_shouldReturnList() {
        // 准备测试数据
        AgentTestCase tc1 = new AgentTestCase();
        tc1.setId(1L);
        AgentTestCase tc2 = new AgentTestCase();
        tc2.setId(2L);

        when(testCaseRepository.findByTenantId(TENANT_ID)).thenReturn(Arrays.asList(tc1, tc2));

        // 执行测试
        List<AgentTestCase> result = testCaseService.getTestCasesByTenantId(TENANT_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(testCaseRepository).findByTenantId(TENANT_ID);
    }

    // ==================== getTestCasesByAgentId 测试 ====================

    @Test
    @DisplayName("根据AgentID获取测试用例列表 - 成功返回列表")
    void getTestCasesByAgentId_shouldReturnList() {
        // 准备测试数据
        AgentTestCase tc1 = new AgentTestCase();
        tc1.setId(1L);
        tc1.setAgentId(AGENT_ID);
        AgentTestCase tc2 = new AgentTestCase();
        tc2.setId(2L);
        tc2.setAgentId(AGENT_ID);

        when(testCaseRepository.findByAgentId(AGENT_ID)).thenReturn(Arrays.asList(tc1, tc2));

        // 执行测试
        List<AgentTestCase> result = testCaseService.getTestCasesByAgentId(AGENT_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(testCaseRepository).findByAgentId(AGENT_ID);
    }

    // ==================== updateTestCase 测试 ====================

    @Test
    @DisplayName("更新测试用例 - 成功更新用例信息")
    void updateTestCase_shouldUpdateSuccessfully() {
        // 准备测试数据
        AgentTestCase existing = new AgentTestCase();
        existing.setId(TEST_CASE_ID);
        existing.setTestName("旧名称");
        existing.setDescription("旧描述");
        existing.setTestType("unit");
        existing.setInputParams("{}");
        existing.setExpectedOutput("{}");
        existing.setStatus(1);

        AgentTestCase updated = new AgentTestCase();
        updated.setTestName("新名称");
        updated.setDescription("新描述");
        updated.setTestType("integration");
        updated.setInputParams("{\"key\":\"value\"}");
        updated.setExpectedOutput("{\"result\":\"ok\"}");
        updated.setStatus(2);

        when(testCaseRepository.findById(TEST_CASE_ID)).thenReturn(Optional.of(existing));
        when(testCaseRepository.save(any(AgentTestCase.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        AgentTestCase result = testCaseService.updateTestCase(TEST_CASE_ID, updated);

        // 验证结果
        assertNotNull(result);
        assertEquals("新名称", result.getTestName());
        assertEquals("新描述", result.getDescription());
        assertEquals("integration", result.getTestType());
        assertEquals("{\"key\":\"value\"}", result.getInputParams());
        assertEquals("{\"result\":\"ok\"}", result.getExpectedOutput());
        assertEquals(2, result.getStatus());
        verify(testCaseRepository).save(existing);
    }

    @Test
    @DisplayName("更新测试用例 - 用例不存在时抛出异常")
    void updateTestCase_shouldThrowExceptionWhenNotFound() {
        // 模拟用例不存在
        AgentTestCase updated = new AgentTestCase();
        updated.setTestName("新名称");

        when(testCaseRepository.findById(TEST_CASE_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> testCaseService.updateTestCase(TEST_CASE_ID, updated));

        assertEquals("Test case not found", exception.getMessage());
        verify(testCaseRepository, never()).save(any());
    }

    // ==================== deleteTestCase 测试 ====================

    @Test
    @DisplayName("删除测试用例 - 成功删除用例")
    void deleteTestCase_shouldDeleteSuccessfully() {
        // 准备测试数据
        AgentTestCase testCase = new AgentTestCase();
        testCase.setId(TEST_CASE_ID);
        testCase.setTestName("待删除用例");

        when(testCaseRepository.findById(TEST_CASE_ID)).thenReturn(Optional.of(testCase));

        // 执行测试
        testCaseService.deleteTestCase(TEST_CASE_ID);

        // 验证结果
        verify(testCaseRepository).delete(testCase);
    }

    @Test
    @DisplayName("删除测试用例 - 用例不存在时抛出异常")
    void deleteTestCase_shouldThrowExceptionWhenNotFound() {
        // 模拟用例不存在
        when(testCaseRepository.findById(TEST_CASE_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> testCaseService.deleteTestCase(TEST_CASE_ID));

        assertEquals("Test case not found", exception.getMessage());
        verify(testCaseRepository, never()).delete(any());
    }

    // ==================== getTestCaseByCode 测试 ====================

    @Test
    @DisplayName("根据编码获取测试用例 - 成功返回用例")
    void getTestCaseByCode_shouldReturnTestCase() {
        // 准备测试数据
        AgentTestCase testCase = new AgentTestCase();
        testCase.setId(TEST_CASE_ID);
        testCase.setTestCode("TC_LOGIN_001");

        when(testCaseRepository.findByTenantIdAndTestCode(TENANT_ID, "TC_LOGIN_001"))
                .thenReturn(testCase);

        // 执行测试
        AgentTestCase result = testCaseService.getTestCaseByCode(TENANT_ID, "TC_LOGIN_001");

        // 验证结果
        assertNotNull(result);
        assertEquals("TC_LOGIN_001", result.getTestCode());
    }

    // ==================== getTestCasesByStatus 测试 ====================

    @Test
    @DisplayName("根据状态获取测试用例列表 - 成功返回列表")
    void getTestCasesByStatus_shouldReturnList() {
        // 准备测试数据
        AgentTestCase tc = new AgentTestCase();
        tc.setId(1L);
        tc.setStatus(1);

        when(testCaseRepository.findByTenantIdAndStatus(TENANT_ID, 1)).thenReturn(List.of(tc));

        // 执行测试
        List<AgentTestCase> result = testCaseService.getTestCasesByStatus(TENANT_ID, 1);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(testCaseRepository).findByTenantIdAndStatus(TENANT_ID, 1);
    }

    // ==================== countTestCasesByTenant 测试 ====================

    @Test
    @DisplayName("统计租户下的测试用例数量 - 成功返回数量")
    void countTestCasesByTenant_shouldReturnCount() {
        when(testCaseRepository.countByTenantId(TENANT_ID)).thenReturn(5L);

        // 执行测试
        long count = testCaseService.countTestCasesByTenant(TENANT_ID);

        // 验证结果
        assertEquals(5L, count);
        verify(testCaseRepository).countByTenantId(TENANT_ID);
    }

    // ==================== countTestCasesByAgent 测试 ====================

    @Test
    @DisplayName("统计Agent下的测试用例数量 - 成功返回数量")
    void countTestCasesByAgent_shouldReturnCount() {
        when(testCaseRepository.countByAgentId(AGENT_ID)).thenReturn(3L);

        // 执行测试
        long count = testCaseService.countTestCasesByAgent(AGENT_ID);

        // 验证结果
        assertEquals(3L, count);
        verify(testCaseRepository).countByAgentId(AGENT_ID);
    }
}
