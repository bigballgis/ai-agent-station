package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.Result;
import com.aiagent.dto.CreateExecutionRequestDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.ExecutionResponseDTO;
import com.aiagent.entity.AgentTestExecution;
import com.aiagent.service.AgentTestExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/test-executions")
@RequiredArgsConstructor
@Tag(name = "测试执行管理", description = "Agent测试执行管理接口")
public class AgentTestExecutionController {

    private final AgentTestExecutionService executionService;

    @RequiresPermission("test:execute")
    @PostMapping
    @Operation(summary = "创建测试执行")
    public Result<ExecutionResponseDTO> createExecution(@RequestBody CreateExecutionRequestDTO request) {
        AgentTestExecution execution = DTOConverter.toExecutionEntity(request);
        AgentTestExecution createdExecution = executionService.createExecution(execution);
        return Result.success(DTOConverter.toExecutionResponseDTO(createdExecution));
    }

    @RequiresPermission("test:execute")
    @Operation(summary = "创建测试执行")
    @PostMapping("/{id}/execute")
    public Result<ExecutionResponseDTO> executeTest(@PathVariable Long id) {
        AgentTestExecution execution = executionService.executeTest(id);
        return Result.success(DTOConverter.toExecutionResponseDTO(execution));
    }

    @RequiresPermission("test:read")
    @Operation(summary = "执行测试")
    @GetMapping("/{id}")
    public Result<ExecutionResponseDTO> getExecutionById(@PathVariable Long id) {
        return executionService.getExecutionById(id)
                .map(DTOConverter::toExecutionResponseDTO)
                .map(Result::success)
                .orElse(Result.fail("Test execution not found"));
    }

    @Operation(summary = "根据ID获取测试执行详情")
    @GetMapping("/tenant/{tenantId}")
    public Result<List<ExecutionResponseDTO>> getExecutionsByTenantId(@PathVariable Long tenantId) {
        List<AgentTestExecution> executions = executionService.getExecutionsByTenantId(tenantId);
        List<ExecutionResponseDTO> dtoList = executions.stream()
                .map(DTOConverter::toExecutionResponseDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @Operation(summary = "根据租户ID获取测试执行列表")
    @GetMapping("/agent/{agentId}")
    public Result<List<ExecutionResponseDTO>> getExecutionsByAgentId(@PathVariable Long agentId) {
        List<AgentTestExecution> executions = executionService.getExecutionsByAgentId(agentId);
        List<ExecutionResponseDTO> dtoList = executions.stream()
                .map(DTOConverter::toExecutionResponseDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @Operation(summary = "根据Agent ID获取测试执行列表")
    @GetMapping("/test-case/{testCaseId}")
    public Result<List<ExecutionResponseDTO>> getExecutionsByTestCaseId(@PathVariable Long testCaseId) {
        List<AgentTestExecution> executions = executionService.getExecutionsByTestCaseId(testCaseId);
        List<ExecutionResponseDTO> dtoList = executions.stream()
                .map(DTOConverter::toExecutionResponseDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @Operation(summary = "根据测试用例ID获取执行列表")
    @GetMapping("/status/{tenantId}/{status}")
    public Result<List<ExecutionResponseDTO>> getExecutionsByStatus(@PathVariable Long tenantId, @PathVariable Integer status) {
        List<AgentTestExecution> executions = executionService.getExecutionsByStatus(tenantId, status);
        List<ExecutionResponseDTO> dtoList = executions.stream()
                .map(DTOConverter::toExecutionResponseDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @Operation(summary = "根据状态获取测试执行列表")
    @GetMapping("/type/{tenantId}/{executionType}")
    public Result<List<ExecutionResponseDTO>> getExecutionsByType(@PathVariable Long tenantId, @PathVariable String executionType) {
        List<AgentTestExecution> executions = executionService.getExecutionsByType(tenantId, executionType);
        List<ExecutionResponseDTO> dtoList = executions.stream()
                .map(DTOConverter::toExecutionResponseDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @Operation(summary = "根据类型获取测试执行列表")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelExecution(@PathVariable Long id) {
        executionService.cancelExecution(id);
        return Result.success();
    }

    @Operation(summary = "取消测试执行")
    @GetMapping("/count/tenant/{tenantId}")
    public Result<Long> countExecutionsByTenant(@PathVariable Long tenantId) {
        long count = executionService.countExecutionsByTenant(tenantId);
        return Result.success(count);
    }

    @Operation(summary = "统计租户下测试执行数量")
    @GetMapping("/count/agent/{agentId}")
    public Result<Long> countExecutionsByAgent(@PathVariable Long agentId) {
        long count = executionService.countExecutionsByAgent(agentId);
        return Result.success(count);
    }

    @Operation(summary = "统计Agent下测试执行数量")
    @GetMapping("/count/test-case/{testCaseId}")
    public Result<Long> countExecutionsByTestCase(@PathVariable Long testCaseId) {
        long count = executionService.countExecutionsByTestCase(testCaseId);
        return Result.success(count);
    }
}
