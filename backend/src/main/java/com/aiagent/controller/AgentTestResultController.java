package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.Result;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.service.AgentTestResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/test-results")
@RequiredArgsConstructor
@Tag(name = "测试结果管理", description = "Agent测试结果管理接口")
public class AgentTestResultController {

    private final AgentTestResultService resultService;

    @RequiresPermission("test:view")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取测试结果详情")
    public Result<AgentTestResult> getResultById(@PathVariable Long id) {
        return resultService.getResultById(id)
                .map(Result::success)
                .orElse(Result.fail("Test result not found"));
    }

    @Operation(summary = "根据ID获取测试结果详情")
    @RequiresPermission("test:view")
    @GetMapping("/execution/{executionId}")
    public Result<List<AgentTestResult>> getResultsByExecutionId(@PathVariable Long executionId) {
        List<AgentTestResult> results = resultService.getResultsByExecutionId(executionId);
        return Result.success(results);
    }

    @Operation(summary = "根据执行ID获取测试结果列表")
    @RequiresPermission("test:view")
    @GetMapping("/tenant/{tenantId}")
    public Result<List<AgentTestResult>> getResultsByTenantId(@PathVariable Long tenantId) {
        List<AgentTestResult> results = resultService.getResultsByTenantId(tenantId);
        return Result.success(results);
    }

    @Operation(summary = "根据租户ID获取测试结果列表")
    @RequiresPermission("test:view")
    @GetMapping("/agent/{agentId}")
    public Result<List<AgentTestResult>> getResultsByAgentId(@PathVariable Long agentId) {
        List<AgentTestResult> results = resultService.getResultsByAgentId(agentId);
        return Result.success(results);
    }

    @Operation(summary = "根据Agent ID获取测试结果列表")
    @RequiresPermission("test:view")
    @GetMapping("/test-case/{testCaseId}")
    public Result<List<AgentTestResult>> getResultsByTestCaseId(@PathVariable Long testCaseId) {
        List<AgentTestResult> results = resultService.getResultsByTestCaseId(testCaseId);
        return Result.success(results);
    }

    @Operation(summary = "根据测试用例ID获取结果列表")
    @RequiresPermission("test:view")
    @GetMapping("/status/{tenantId}/{status}")
    public Result<List<AgentTestResult>> getResultsByStatus(@PathVariable Long tenantId, @PathVariable String status) {
        List<AgentTestResult> results = resultService.getResultsByStatus(tenantId, status);
        return Result.success(results);
    }

    @Operation(summary = "根据状态获取测试结果列表")
    @RequiresPermission("test:manage")
    @PutMapping("/{id}")
    public Result<AgentTestResult> updateResult(@PathVariable Long id, @RequestBody AgentTestResult result) {
        AgentTestResult updatedResult = resultService.updateResult(id, result);
        return Result.success(updatedResult);
    }

    @Operation(summary = "更新测试结果")
    @RequiresPermission("test:view")
    @GetMapping("/count/tenant/{tenantId}")
    public Result<Long> countResultsByTenant(@PathVariable Long tenantId) {
        long count = resultService.countResultsByTenant(tenantId);
        return Result.success(count);
    }

    @Operation(summary = "统计租户下测试结果数量")
    @RequiresPermission("test:view")
    @GetMapping("/count/agent/{agentId}")
    public Result<Long> countResultsByAgent(@PathVariable Long agentId) {
        long count = resultService.countResultsByAgent(agentId);
        return Result.success(count);
    }

    @Operation(summary = "统计Agent下测试结果数量")
    @RequiresPermission("test:view")
    @GetMapping("/count/test-case/{testCaseId}")
    public Result<Long> countResultsByTestCase(@PathVariable Long testCaseId) {
        long count = resultService.countResultsByTestCase(testCaseId);
        return Result.success(count);
    }

    @Operation(summary = "统计测试用例下结果数量")
    @RequiresPermission("test:view")
    @GetMapping("/count/execution/{executionId}")
    public Result<Long> countResultsByExecution(@PathVariable Long executionId) {
        long count = resultService.countResultsByExecution(executionId);
        return Result.success(count);
    }

    @Operation(summary = "统计执行下结果数量")
    @RequiresPermission("test:view")
    @GetMapping("/pass-rate/agent/{agentId}")
    public Result<Double> getPassRateByAgent(@PathVariable Long agentId) {
        double passRate = resultService.getPassRateByAgent(agentId);
        return Result.success(passRate);
    }

    @Operation(summary = "获取Agent通过率")
    @RequiresPermission("test:view")
    @GetMapping("/pass-rate/test-case/{testCaseId}")
    public Result<Double> getPassRateByTestCase(@PathVariable Long testCaseId) {
        double passRate = resultService.getPassRateByTestCase(testCaseId);
        return Result.success(passRate);
    }
}
