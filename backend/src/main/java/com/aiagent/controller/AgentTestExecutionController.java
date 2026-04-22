package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.entity.AgentTestExecution;
import com.aiagent.service.AgentTestExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test-executions")
@RequiredArgsConstructor
public class AgentTestExecutionController {

    private final AgentTestExecutionService executionService;

    @PostMapping
    public Result<AgentTestExecution> createExecution(@RequestBody AgentTestExecution execution) {
        AgentTestExecution createdExecution = executionService.createExecution(execution);
        return Result.success(createdExecution);
    }

    @PostMapping("/{id}/execute")
    public Result<AgentTestExecution> executeTest(@PathVariable Long id) {
        AgentTestExecution execution = executionService.executeTest(id);
        return Result.success(execution);
    }

    @GetMapping("/{id}")
    public Result<AgentTestExecution> getExecutionById(@PathVariable Long id) {
        return executionService.getExecutionById(id)
                .map(Result::success)
                .orElse(Result.fail("Test execution not found"));
    }

    @GetMapping("/tenant/{tenantId}")
    public Result<List<AgentTestExecution>> getExecutionsByTenantId(@PathVariable Long tenantId) {
        List<AgentTestExecution> executions = executionService.getExecutionsByTenantId(tenantId);
        return Result.success(executions);
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<AgentTestExecution>> getExecutionsByAgentId(@PathVariable Long agentId) {
        List<AgentTestExecution> executions = executionService.getExecutionsByAgentId(agentId);
        return Result.success(executions);
    }

    @GetMapping("/test-case/{testCaseId}")
    public Result<List<AgentTestExecution>> getExecutionsByTestCaseId(@PathVariable Long testCaseId) {
        List<AgentTestExecution> executions = executionService.getExecutionsByTestCaseId(testCaseId);
        return Result.success(executions);
    }

    @GetMapping("/status/{tenantId}/{status}")
    public Result<List<AgentTestExecution>> getExecutionsByStatus(@PathVariable Long tenantId, @PathVariable Integer status) {
        List<AgentTestExecution> executions = executionService.getExecutionsByStatus(tenantId, status);
        return Result.success(executions);
    }

    @GetMapping("/type/{tenantId}/{executionType}")
    public Result<List<AgentTestExecution>> getExecutionsByType(@PathVariable Long tenantId, @PathVariable String executionType) {
        List<AgentTestExecution> executions = executionService.getExecutionsByType(tenantId, executionType);
        return Result.success(executions);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelExecution(@PathVariable Long id) {
        executionService.cancelExecution(id);
        return Result.success();
    }

    @GetMapping("/count/tenant/{tenantId}")
    public Result<Long> countExecutionsByTenant(@PathVariable Long tenantId) {
        long count = executionService.countExecutionsByTenant(tenantId);
        return Result.success(count);
    }

    @GetMapping("/count/agent/{agentId}")
    public Result<Long> countExecutionsByAgent(@PathVariable Long agentId) {
        long count = executionService.countExecutionsByAgent(agentId);
        return Result.success(count);
    }

    @GetMapping("/count/test-case/{testCaseId}")
    public Result<Long> countExecutionsByTestCase(@PathVariable Long testCaseId) {
        long count = executionService.countExecutionsByTestCase(testCaseId);
        return Result.success(count);
    }
}
