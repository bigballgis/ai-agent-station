package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.service.AgentTestResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test-results")
@RequiredArgsConstructor
public class AgentTestResultController {

    private final AgentTestResultService resultService;

    @GetMapping("/{id}")
    public Result<AgentTestResult> getResultById(@PathVariable Long id) {
        return resultService.getResultById(id)
                .map(Result::success)
                .orElse(Result.fail("Test result not found"));
    }

    @GetMapping("/execution/{executionId}")
    public Result<List<AgentTestResult>> getResultsByExecutionId(@PathVariable Long executionId) {
        List<AgentTestResult> results = resultService.getResultsByExecutionId(executionId);
        return Result.success(results);
    }

    @GetMapping("/tenant/{tenantId}")
    public Result<List<AgentTestResult>> getResultsByTenantId(@PathVariable Long tenantId) {
        List<AgentTestResult> results = resultService.getResultsByTenantId(tenantId);
        return Result.success(results);
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<AgentTestResult>> getResultsByAgentId(@PathVariable Long agentId) {
        List<AgentTestResult> results = resultService.getResultsByAgentId(agentId);
        return Result.success(results);
    }

    @GetMapping("/test-case/{testCaseId}")
    public Result<List<AgentTestResult>> getResultsByTestCaseId(@PathVariable Long testCaseId) {
        List<AgentTestResult> results = resultService.getResultsByTestCaseId(testCaseId);
        return Result.success(results);
    }

    @GetMapping("/status/{tenantId}/{status}")
    public Result<List<AgentTestResult>> getResultsByStatus(@PathVariable Long tenantId, @PathVariable String status) {
        List<AgentTestResult> results = resultService.getResultsByStatus(tenantId, status);
        return Result.success(results);
    }

    @PutMapping("/{id}")
    public Result<AgentTestResult> updateResult(@PathVariable Long id, @RequestBody AgentTestResult result) {
        AgentTestResult updatedResult = resultService.updateResult(id, result);
        return Result.success(updatedResult);
    }

    @GetMapping("/count/tenant/{tenantId}")
    public Result<Long> countResultsByTenant(@PathVariable Long tenantId) {
        long count = resultService.countResultsByTenant(tenantId);
        return Result.success(count);
    }

    @GetMapping("/count/agent/{agentId}")
    public Result<Long> countResultsByAgent(@PathVariable Long agentId) {
        long count = resultService.countResultsByAgent(agentId);
        return Result.success(count);
    }

    @GetMapping("/count/test-case/{testCaseId}")
    public Result<Long> countResultsByTestCase(@PathVariable Long testCaseId) {
        long count = resultService.countResultsByTestCase(testCaseId);
        return Result.success(count);
    }

    @GetMapping("/count/execution/{executionId}")
    public Result<Long> countResultsByExecution(@PathVariable Long executionId) {
        long count = resultService.countResultsByExecution(executionId);
        return Result.success(count);
    }

    @GetMapping("/pass-rate/agent/{agentId}")
    public Result<Double> getPassRateByAgent(@PathVariable Long agentId) {
        double passRate = resultService.getPassRateByAgent(agentId);
        return Result.success(passRate);
    }

    @GetMapping("/pass-rate/test-case/{testCaseId}")
    public Result<Double> getPassRateByTestCase(@PathVariable Long testCaseId) {
        double passRate = resultService.getPassRateByTestCase(testCaseId);
        return Result.success(passRate);
    }
}
