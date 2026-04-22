package com.aiagent.service;

import com.aiagent.entity.AgentTestExecution;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.engine.TestExecutionEngine;
import com.aiagent.repository.AgentTestExecutionRepository;
import com.aiagent.repository.AgentTestResultRepository;
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
 * AgentTestExecutionService 单元测试
 * 测试测试执行服务的核心方法：创建执行、执行测试、取消执行、查询执行等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent测试执行服务测试")
class AgentTestExecutionServiceTest {

    @Mock
    private AgentTestExecutionRepository executionRepository;

    @Mock
    private AgentTestResultRepository resultRepository;

    @Mock
    private TestExecutionEngine testExecutionEngine;

    @InjectMocks
    private AgentTestExecutionService executionService;

    private static final Long EXECUTION_ID = 1L;
    private static final Long AGENT_ID = 100L;
    private static final Long TEST_CASE_ID = 200L;
    private static final Long TENANT_ID = 1L;

    // ==================== createExecution 测试 ====================

    @Test
    @DisplayName("创建测试执行 - 成功创建并设置初始状态和时间")
    void createExecution_shouldCreateSuccessfully() {
        // 准备测试数据
        AgentTestExecution execution = new AgentTestExecution();
        execution.setAgentId(AGENT_ID);
        execution.setTestCaseId(TEST_CASE_ID);
        execution.setTenantId(TENANT_ID);

        when(executionRepository.save(any(AgentTestExecution.class))).thenAnswer(inv -> {
            AgentTestExecution saved = inv.getArgument(0);
            saved.setId(EXECUTION_ID);
            return saved;
        });

        // 执行测试
        AgentTestExecution result = executionService.createExecution(execution);

        // 验证结果
        assertNotNull(result);
        assertEquals(EXECUTION_ID, result.getId());
        assertEquals(0, result.getStatus()); // 初始状态为Pending
        assertNotNull(result.getStartTime());
        verify(executionRepository).save(execution);
    }

    // ==================== executeTest 测试 ====================

    @Test
    @DisplayName("执行测试 - 成功启动测试执行")
    void executeTest_shouldStartExecution() {
        // 准备测试数据
        AgentTestExecution execution = new AgentTestExecution();
        execution.setId(EXECUTION_ID);
        execution.setStatus(0); // Pending

        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
        when(executionRepository.save(any(AgentTestExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        AgentTestExecution result = executionService.executeTest(EXECUTION_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getStatus()); // 状态变为Running
        verify(executionRepository).save(execution);
    }

    @Test
    @DisplayName("执行测试 - 执行记录不存在时抛出异常")
    void executeTest_shouldThrowExceptionWhenNotFound() {
        // 模拟执行记录不存在
        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> executionService.executeTest(EXECUTION_ID));

        assertEquals("Test execution not found", exception.getMessage());
    }

    // ==================== executeTestAsync 测试 ====================

    @Test
    @DisplayName("异步执行测试 - 成功执行并更新状态为完成")
    void executeTestAsync_shouldCompleteSuccessfully() {
        // 准备测试数据
        AgentTestExecution execution = new AgentTestExecution();
        execution.setId(EXECUTION_ID);
        execution.setAgentId(AGENT_ID);

        AgentTestResult testResult = new AgentTestResult();
        testResult.setExecutionId(EXECUTION_ID);

        when(testExecutionEngine.executeTest(execution)).thenReturn(testResult);
        when(executionRepository.save(any(AgentTestExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        executionService.executeTestAsync(execution);

        // 验证结果
        verify(testExecutionEngine).executeTest(execution);
        verify(resultRepository).save(testResult);
        assertEquals(2, execution.getStatus()); // Completed
        verify(executionRepository).save(execution);
    }

    @Test
    @DisplayName("异步执行测试 - 执行失败时更新状态为失败并设置错误信息")
    void executeTestAsync_shouldHandleFailure() {
        // 准备测试数据
        AgentTestExecution execution = new AgentTestExecution();
        execution.setId(EXECUTION_ID);
        execution.setAgentId(AGENT_ID);

        when(testExecutionEngine.executeTest(execution))
                .thenThrow(new RuntimeException("执行超时"));
        when(executionRepository.save(any(AgentTestExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        executionService.executeTestAsync(execution);

        // 验证结果
        assertEquals(3, execution.getStatus()); // Failed
        assertEquals("执行超时", execution.getErrorMessage());
        verify(resultRepository, never()).save(any());
    }

    // ==================== getExecutionById 测试 ====================

    @Test
    @DisplayName("根据ID获取执行记录 - 成功返回执行记录")
    void getExecutionById_shouldReturnExecution() {
        // 准备测试数据
        AgentTestExecution execution = new AgentTestExecution();
        execution.setId(EXECUTION_ID);
        execution.setStatus(2);

        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));

        // 执行测试
        Optional<AgentTestExecution> result = executionService.getExecutionById(EXECUTION_ID);

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(EXECUTION_ID, result.get().getId());
    }

    @Test
    @DisplayName("根据ID获取执行记录 - 记录不存在时返回空Optional")
    void getExecutionById_shouldReturnEmptyWhenNotFound() {
        // 模拟记录不存在
        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.empty());

        // 执行测试
        Optional<AgentTestExecution> result = executionService.getExecutionById(EXECUTION_ID);

        // 验证结果
        assertFalse(result.isPresent());
    }

    // ==================== getExecutionsByTenantId 测试 ====================

    @Test
    @DisplayName("根据租户ID获取执行列表 - 成功返回列表")
    void getExecutionsByTenantId_shouldReturnList() {
        // 准备测试数据
        AgentTestExecution exec1 = new AgentTestExecution();
        exec1.setId(1L);
        AgentTestExecution exec2 = new AgentTestExecution();
        exec2.setId(2L);

        when(executionRepository.findByTenantId(TENANT_ID)).thenReturn(Arrays.asList(exec1, exec2));

        // 执行测试
        List<AgentTestExecution> result = executionService.getExecutionsByTenantId(TENANT_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(executionRepository).findByTenantId(TENANT_ID);
    }

    // ==================== getExecutionsByAgentId 测试 ====================

    @Test
    @DisplayName("根据AgentID获取执行列表 - 成功返回列表")
    void getExecutionsByAgentId_shouldReturnList() {
        // 准备测试数据
        AgentTestExecution exec1 = new AgentTestExecution();
        exec1.setId(1L);
        exec1.setAgentId(AGENT_ID);
        AgentTestExecution exec2 = new AgentTestExecution();
        exec2.setId(2L);
        exec2.setAgentId(AGENT_ID);

        when(executionRepository.findByAgentId(AGENT_ID)).thenReturn(Arrays.asList(exec1, exec2));

        // 执行测试
        List<AgentTestExecution> result = executionService.getExecutionsByAgentId(AGENT_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(executionRepository).findByAgentId(AGENT_ID);
    }

    // ==================== getExecutionsByTestCaseId 测试 ====================

    @Test
    @DisplayName("根据测试用例ID获取执行列表 - 成功返回列表")
    void getExecutionsByTestCaseId_shouldReturnList() {
        // 准备测试数据
        AgentTestExecution exec = new AgentTestExecution();
        exec.setId(1L);
        exec.setTestCaseId(TEST_CASE_ID);

        when(executionRepository.findByTestCaseId(TEST_CASE_ID)).thenReturn(List.of(exec));

        // 执行测试
        List<AgentTestExecution> result = executionService.getExecutionsByTestCaseId(TEST_CASE_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(executionRepository).findByTestCaseId(TEST_CASE_ID);
    }

    // ==================== cancelExecution 测试 ====================

    @Test
    @DisplayName("取消测试执行 - 成功取消并设置结束时间")
    void cancelExecution_shouldCancelSuccessfully() {
        // 准备测试数据
        AgentTestExecution execution = new AgentTestExecution();
        execution.setId(EXECUTION_ID);
        execution.setStatus(1); // Running

        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
        when(executionRepository.save(any(AgentTestExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        executionService.cancelExecution(EXECUTION_ID);

        // 验证结果
        assertEquals(4, execution.getStatus()); // Cancelled
        assertNotNull(execution.getEndTime());
        verify(executionRepository).save(execution);
    }

    @Test
    @DisplayName("取消测试执行 - 执行记录不存在时抛出异常")
    void cancelExecution_shouldThrowExceptionWhenNotFound() {
        // 模拟记录不存在
        when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> executionService.cancelExecution(EXECUTION_ID));

        assertEquals("Test execution not found", exception.getMessage());
    }

    // ==================== countExecutionsByTenant 测试 ====================

    @Test
    @DisplayName("统计租户下的执行数量 - 成功返回数量")
    void countExecutionsByTenant_shouldReturnCount() {
        when(executionRepository.countByTenantId(TENANT_ID)).thenReturn(10L);

        // 执行测试
        long count = executionService.countExecutionsByTenant(TENANT_ID);

        // 验证结果
        assertEquals(10L, count);
    }

    // ==================== countExecutionsByAgent 测试 ====================

    @Test
    @DisplayName("统计Agent下的执行数量 - 成功返回数量")
    void countExecutionsByAgent_shouldReturnCount() {
        when(executionRepository.countByAgentId(AGENT_ID)).thenReturn(7L);

        // 执行测试
        long count = executionService.countExecutionsByAgent(AGENT_ID);

        // 验证结果
        assertEquals(7L, count);
    }
}
