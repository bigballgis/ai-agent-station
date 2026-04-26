package com.aiagent.service;

import com.aiagent.entity.AgentTestResult;
import com.aiagent.repository.AgentTestResultRepository;
import com.aiagent.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentTestResultService 单元测试
 * 测试测试结果的创建、查询、通过率计算、更新等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent测试结果服务测试")
class AgentTestResultServiceTest {

    @Mock
    private AgentTestResultRepository resultRepository;

    @InjectMocks
    private AgentTestResultService agentTestResultService;

    private AgentTestResult testResult;

    @BeforeEach
    void setUp() {
        // 初始化测试结果数据
        testResult = new AgentTestResult();
        testResult.setId(1L);
        testResult.setExecutionId(1L);
        testResult.setTenantId(100L);
        testResult.setAgentId(1L);
        testResult.setTestCaseId(1L);
        testResult.setActualOutput("{\"result\": \"hello\"}");
        testResult.setExpectedOutput("{\"result\": \"hello\"}");
        testResult.setStatus("SUCCESS");
        testResult.setComparisonResult("{\"match\": true}");
        testResult.setErrorMessage(null);
    }

    // ==================== createResult / getResultById 测试 ====================

    @Test
    @DisplayName("通过ID获取测试结果 - 成功")
    void getResultById_Success() {
        when(resultRepository.findById(1L)).thenReturn(Optional.of(testResult));

        Optional<AgentTestResult> result = agentTestResultService.getResultById(1L);

        assertTrue(result.isPresent());
        assertEquals("SUCCESS", result.get().getStatus());
        assertEquals(1L, result.get().getExecutionId());
    }

    @Test
    @DisplayName("通过ID获取测试结果 - 不存在")
    void getResultById_NotFound() {
        when(resultRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<AgentTestResult> result = agentTestResultService.getResultById(999L);

        assertFalse(result.isPresent());
    }

    // ==================== getResultsByExecution 测试 ====================

    @Test
    @DisplayName("按执行ID查询测试结果 - 成功")
    void getResultsByExecutionId_Success() {
        List<AgentTestResult> results = Arrays.asList(testResult);
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.findByExecutionIdAndTenantId(1L, 100L)).thenReturn(results);

            List<AgentTestResult> result = agentTestResultService.getResultsByExecutionId(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("SUCCESS", result.get(0).getStatus());
        }
    }

    @Test
    @DisplayName("按执行ID查询测试结果 - 空列表")
    void getResultsByExecutionId_Empty() {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.findByExecutionIdAndTenantId(1L, 100L)).thenReturn(Collections.emptyList());

            List<AgentTestResult> result = agentTestResultService.getResultsByExecutionId(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== getPassRate 测试 ====================

    @Test
    @DisplayName("按Agent计算通过率 - 全部通过")
    void getPassRateByAgent_AllPassed() {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.countByAgentIdAndTenantId(1L, 100L)).thenReturn(2L);
            when(resultRepository.countByAgentIdAndStatus(1L, "SUCCESS")).thenReturn(2L);

            double passRate = agentTestResultService.getPassRateByAgent(1L);
            assertEquals(100.0, passRate, 0.001);
        }
    }

    @Test
    @DisplayName("按Agent计算通过率 - 部分通过")
    void getPassRateByAgent_PartialPassed() {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.countByAgentIdAndTenantId(1L, 100L)).thenReturn(3L);
            when(resultRepository.countByAgentIdAndStatus(1L, "SUCCESS")).thenReturn(2L);

            double passRate = agentTestResultService.getPassRateByAgent(1L);
            // 2/3 * 100 = 66.667
            assertEquals(66.667, passRate, 0.01);
        }
    }

    @Test
    @DisplayName("按Agent计算通过率 - 无测试结果时返回0")
    void getPassRateByAgent_NoResults() {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.countByAgentIdAndTenantId(1L, 100L)).thenReturn(0L);

            double passRate = agentTestResultService.getPassRateByAgent(1L);
            assertEquals(0.0, passRate, 0.001);
        }
    }

    @Test
    @DisplayName("按Agent计算通过率 - 全部失败")
    void getPassRateByAgent_AllFailed() {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.countByAgentIdAndTenantId(1L, 100L)).thenReturn(2L);
            when(resultRepository.countByAgentIdAndStatus(1L, "SUCCESS")).thenReturn(0L);

            double passRate = agentTestResultService.getPassRateByAgent(1L);
            assertEquals(0.0, passRate, 0.001);
        }
    }

    // ==================== updateResult 测试 ====================

    @Test
    @DisplayName("更新测试结果 - 成功")
    void updateResult_Success() {
        AgentTestResult updatedResult = new AgentTestResult();
        updatedResult.setActualOutput("{\"result\": \"updated\"}");
        updatedResult.setExpectedOutput("{\"result\": \"expected\"}");
        updatedResult.setStatus("FAILED");
        updatedResult.setComparisonResult("{\"match\": false}");
        updatedResult.setErrorMessage("输出不匹配");

        when(resultRepository.findById(1L)).thenReturn(Optional.of(testResult));
        when(resultRepository.save(any(AgentTestResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentTestResult result = agentTestResultService.updateResult(1L, updatedResult);

        assertNotNull(result);
        assertEquals("{\"result\": \"updated\"}", result.getActualOutput());
        assertEquals("FAILED", result.getStatus());
        assertEquals("输出不匹配", result.getErrorMessage());
        verify(resultRepository).save(any(AgentTestResult.class));
    }

    @Test
    @DisplayName("更新测试结果 - 结果不存在时抛出异常")
    void updateResult_NotFound_ThrowsException() {
        AgentTestResult updatedResult = new AgentTestResult();
        updatedResult.setStatus("FAILED");

        when(resultRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> agentTestResultService.updateResult(999L, updatedResult));
    }

    // ==================== 其他查询测试 ====================

    @Test
    @DisplayName("按租户查询测试结果 - 成功")
    void getResultsByTenantId_Success() {
        when(resultRepository.findByTenantId(100L)).thenReturn(Arrays.asList(testResult));

        List<AgentTestResult> result = agentTestResultService.getResultsByTenantId(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按Agent查询测试结果 - 成功")
    void getResultsByAgentId_Success() {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.findByAgentIdAndTenantId(1L, 100L)).thenReturn(Arrays.asList(testResult));

            List<AgentTestResult> result = agentTestResultService.getResultsByAgentId(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("按测试用例查询测试结果 - 成功")
    void getResultsByTestCaseId_Success() {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.findByTestCaseIdAndTenantId(1L, 100L)).thenReturn(Arrays.asList(testResult));

            List<AgentTestResult> result = agentTestResultService.getResultsByTestCaseId(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("按租户和状态查询测试结果 - 成功")
    void getResultsByStatus_Success() {
        when(resultRepository.findByTenantIdAndStatus(100L, "SUCCESS"))
                .thenReturn(Arrays.asList(testResult));

        List<AgentTestResult> result = agentTestResultService.getResultsByStatus(100L, "SUCCESS");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("统计租户测试结果数量 - 成功")
    void countResultsByTenant_Success() {
        when(resultRepository.countByTenantId(100L)).thenReturn(42L);

        long count = agentTestResultService.countResultsByTenant(100L);

        assertEquals(42L, count);
    }

    @Test
    @DisplayName("按测试用例计算通过率 - 成功")
    void getPassRateByTestCase_Success() {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentTenantId).thenReturn(100L);
            when(resultRepository.countByTestCaseIdAndTenantId(1L, 100L)).thenReturn(2L);
            when(resultRepository.countByTestCaseIdAndStatus(1L, "SUCCESS")).thenReturn(1L);

            double passRate = agentTestResultService.getPassRateByTestCase(1L);
            assertEquals(50.0, passRate, 0.001);
        }
    }
}
