package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.Result;
import com.aiagent.entity.AgentTestCase;
import com.aiagent.service.AgentTestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/test-cases")
@RequiredArgsConstructor
@Tag(name = "测试用例管理", description = "Agent测试用例管理接口")
public class AgentTestCaseController {

    private final AgentTestCaseService testCaseService;

    @RequiresPermission("test:manage")
    @PostMapping
    public Result<AgentTestCase> createTestCase(@RequestBody AgentTestCase testCase) {
        AgentTestCase createdTestCase = testCaseService.createTestCase(testCase);
        return Result.success(createdTestCase);
    }

    @RequiresPermission("test:view")
    @GetMapping("/{id}")
    public Result<AgentTestCase> getTestCaseById(@PathVariable Long id) {
        return testCaseService.getTestCaseById(id)
                .map(Result::success)
                .orElse(Result.fail("Test case not found"));
    }

    @GetMapping("/tenant/{tenantId}")
    public Result<List<AgentTestCase>> getTestCasesByTenantId(@PathVariable Long tenantId) {
        List<AgentTestCase> testCases = testCaseService.getTestCasesByTenantId(tenantId);
        return Result.success(testCases);
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<AgentTestCase>> getTestCasesByAgentId(@PathVariable Long agentId) {
        List<AgentTestCase> testCases = testCaseService.getTestCasesByAgentId(agentId);
        return Result.success(testCases);
    }

    @PutMapping("/{id}")
    public Result<AgentTestCase> updateTestCase(@PathVariable Long id, @RequestBody AgentTestCase testCase) {
        AgentTestCase updatedTestCase = testCaseService.updateTestCase(id, testCase);
        return Result.success(updatedTestCase);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return Result.success();
    }

    @GetMapping("/code/{tenantId}/{testCode}")
    public Result<AgentTestCase> getTestCaseByCode(@PathVariable Long tenantId, @PathVariable String testCode) {
        AgentTestCase testCase = testCaseService.getTestCaseByCode(tenantId, testCode);
        return testCase != null ? Result.success(testCase) : Result.fail("Test case not found");
    }

    @GetMapping("/status/{tenantId}/{status}")
    public Result<List<AgentTestCase>> getTestCasesByStatus(@PathVariable Long tenantId, @PathVariable Integer status) {
        List<AgentTestCase> testCases = testCaseService.getTestCasesByStatus(tenantId, status);
        return Result.success(testCases);
    }

    @GetMapping("/type/{tenantId}/{testType}")
    public Result<List<AgentTestCase>> getTestCasesByType(@PathVariable Long tenantId, @PathVariable String testType) {
        List<AgentTestCase> testCases = testCaseService.getTestCasesByType(tenantId, testType);
        return Result.success(testCases);
    }

    @GetMapping("/count/tenant/{tenantId}")
    public Result<Long> countTestCasesByTenant(@PathVariable Long tenantId) {
        long count = testCaseService.countTestCasesByTenant(tenantId);
        return Result.success(count);
    }

    @GetMapping("/count/agent/{agentId}")
    public Result<Long> countTestCasesByAgent(@PathVariable Long agentId) {
        long count = testCaseService.countTestCasesByAgent(agentId);
        return Result.success(count);
    }
}
