package com.aiagent.engine;

import com.aiagent.dto.AgentInvokeRequest;
import com.aiagent.dto.AgentInvokeResponse;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentTestCase;
import com.aiagent.entity.AgentTestExecution;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentTestCaseRepository;
import com.aiagent.service.TestDataCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class TestExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionEngine.class);

    private final AgentExecutionEngine agentExecutionEngine;
    private final AgentRepository agentRepository;
    private final AgentTestCaseRepository testCaseRepository;
    private final TestDataCleanupService testDataCleanupService;

    public TestExecutionEngine(AgentExecutionEngine agentExecutionEngine, AgentRepository agentRepository, AgentTestCaseRepository testCaseRepository, TestDataCleanupService testDataCleanupService) {
        this.agentExecutionEngine = agentExecutionEngine;
        this.agentRepository = agentRepository;
        this.testCaseRepository = testCaseRepository;
        this.testDataCleanupService = testDataCleanupService;
    }

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentMap<String, Future<?>> runningTests = new ConcurrentHashMap<>();

    public AgentTestResult executeTest(AgentTestExecution execution) {
        long startTime = System.currentTimeMillis();
        AgentTestResult result = new AgentTestResult();
        result.setExecutionId(execution.getId());
        result.setTenantId(execution.getTenantId());
        result.setAgentId(execution.getAgentId());
        result.setTestCaseId(execution.getTestCaseId());

        try {
            // 准备测试数据隔离
            testDataCleanupService.cleanupTestData(execution.getTenantId(), execution.getAgentId());

            // 获取测试用例
            Optional<AgentTestCase> testCaseOptional = testCaseRepository.findById(execution.getTestCaseId());
            if (!testCaseOptional.isPresent()) {
                throw new RuntimeException("Test case not found");
            }
            AgentTestCase testCase = testCaseOptional.get();

            // 获取Agent
            Optional<Agent> agentOptional = agentRepository.findById(execution.getAgentId());
            if (!agentOptional.isPresent()) {
                throw new RuntimeException("Agent not found");
            }
            Agent agent = agentOptional.get();

            // 构建测试请求
            AgentInvokeRequest request = new AgentInvokeRequest();
            request.setAsync(false);
            request.setInputs(Map.of("testData", testCase.getInputParams()));

            // 执行测试（带超时处理）
            String testId = UUID.randomUUID().toString();
            Future<AgentInvokeResponse> future = executorService.submit(() -> {
                return agentExecutionEngine.invokeAgent(
                        agent.getId(),
                        request,
                        testId,
                        execution.getTenantId()
                );
            });

            runningTests.put(testId, future);

            // 超时设置为30秒
            AgentInvokeResponse response;
            try {
                response = future.get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.error("Test execution timed out", e);
                future.cancel(true);
                throw new RuntimeException("Test execution timed out");
            } finally {
                runningTests.remove(testId);
            }

            // 处理测试结果
            result.setActualOutput(response.getOutputs().toString());
            result.setExpectedOutput(testCase.getExpectedOutput());
            result.setStatus(response.getStatus());

            // 清理测试数据
            testDataCleanupService.cleanupTestData(execution.getTenantId(), execution.getAgentId());

        } catch (Exception e) {
            log.error("Error executing test", e);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
        } finally {
            execution.setEndTime(LocalDateTime.now());
            execution.setExecutionTime((int) (System.currentTimeMillis() - startTime));
        }

        return result;
    }

    @Async
    public void executeTestAsync(AgentTestExecution execution, CompletableFuture<AgentTestResult> callback) {
        try {
            AgentTestResult result = executeTest(execution);
            callback.complete(result);
        } catch (Exception e) {
            callback.completeExceptionally(e);
        }
    }

    public void cancelTest(String testId) {
        Future<?> future = runningTests.get(testId);
        if (future != null) {
            future.cancel(true);
            runningTests.remove(testId);
        }
    }

    public boolean isTestRunning(String testId) {
        Future<?> future = runningTests.get(testId);
        return future != null && !future.isDone();
    }
}
