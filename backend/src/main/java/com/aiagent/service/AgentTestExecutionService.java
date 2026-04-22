package com.aiagent.service;

import com.aiagent.entity.AgentTestExecution;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.engine.TestExecutionEngine;
import com.aiagent.repository.AgentTestExecutionRepository;
import com.aiagent.repository.AgentTestResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AgentTestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentTestExecutionService.class);
    private final AgentTestExecutionRepository executionRepository;
    private final AgentTestResultRepository resultRepository;
    private final TestExecutionEngine testExecutionEngine;

    public AgentTestExecutionService(AgentTestExecutionRepository executionRepository, AgentTestResultRepository resultRepository, TestExecutionEngine testExecutionEngine) {
        this.executionRepository = executionRepository;
        this.resultRepository = resultRepository;
        this.testExecutionEngine = testExecutionEngine;
    }

    @Transactional
    public AgentTestExecution createExecution(AgentTestExecution execution) {
        log.info("Creating test execution for test case: {}", execution.getTestCaseId());
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus(0); // 0: Pending
        return executionRepository.save(execution);
    }

    @Transactional
    public AgentTestExecution executeTest(Long executionId) {
        AgentTestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Test execution not found"));

        execution.setStatus(1); // 1: Running
        executionRepository.save(execution);

        // 执行测试
        executeTestAsync(execution);

        return execution;
    }

    @Async
    public void executeTestAsync(AgentTestExecution execution) {
        try {
            // 使用测试执行引擎执行测试
            AgentTestResult result = testExecutionEngine.executeTest(execution);

            // 保存测试结果
            resultRepository.save(result);

            // 更新执行状态
            execution.setStatus(2); // 2: Completed

        } catch (Exception e) {
            log.error("Error executing test", e);
            execution.setStatus(3); // 3: Failed
            execution.setErrorMessage(e.getMessage());
        } finally {
            executionRepository.save(execution);
        }
    }

    @Transactional(readOnly = true)
    public Optional<AgentTestExecution> getExecutionById(Long id) {
        return executionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<AgentTestExecution> getExecutionsByTenantId(Long tenantId) {
        return executionRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<AgentTestExecution> getExecutionsByAgentId(Long agentId) {
        return executionRepository.findByAgentId(agentId);
    }

    @Transactional(readOnly = true)
    public List<AgentTestExecution> getExecutionsByTestCaseId(Long testCaseId) {
        return executionRepository.findByTestCaseId(testCaseId);
    }

    @Transactional(readOnly = true)
    public List<AgentTestExecution> getExecutionsByStatus(Long tenantId, Integer status) {
        return executionRepository.findByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true)
    public List<AgentTestExecution> getExecutionsByType(Long tenantId, String executionType) {
        return executionRepository.findByTenantIdAndExecutionType(tenantId, executionType);
    }

    @Transactional
    public void cancelExecution(Long id) {
        AgentTestExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test execution not found"));
        execution.setStatus(4); // 4: Cancelled
        execution.setEndTime(LocalDateTime.now());
        executionRepository.save(execution);
    }

    @Transactional(readOnly = true)
    public long countExecutionsByTenant(Long tenantId) {
        return executionRepository.countByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public long countExecutionsByAgent(Long agentId) {
        return executionRepository.countByAgentId(agentId);
    }

    @Transactional(readOnly = true)
    public long countExecutionsByTestCase(Long testCaseId) {
        return executionRepository.countByTestCaseId(testCaseId);
    }
}
