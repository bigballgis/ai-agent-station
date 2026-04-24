package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.CreateExecutionRequestDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.ExecutionResponseDTO;
import com.aiagent.entity.AgentTestExecution;
import com.aiagent.service.AgentTestExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public Result<ExecutionResponseDTO> createExecution(@Valid @RequestBody CreateExecutionRequestDTO request) {
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

    @RequiresPermission("test:view")
    @Operation(summary = "执行测试")
    @GetMapping("/{id}")
    public Result<ExecutionResponseDTO> getExecutionById(@PathVariable Long id) {
        return executionService.getExecutionById(id)
                .map(DTOConverter::toExecutionResponseDTO)
                .map(Result::success)
                .orElse(Result.error("Test execution not found"));
    }

    @RequiresPermission("test:view")
    @Operation(summary = "根据租户ID获取测试执行列表")
    @GetMapping("/tenant/{tenantId}")
    public Result<PageResult<ExecutionResponseDTO>> getExecutionsByTenantId(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestExecution> executionPage = executionService.getExecutionsByTenantId(tenantId, page, size);
        return Result.success(PageResult.from(executionPage.map(DTOConverter::toExecutionResponseDTO)));
    }

    @RequiresPermission("test:view")
    @Operation(summary = "根据Agent ID获取测试执行列表")
    @GetMapping("/agent/{agentId}")
    public Result<PageResult<ExecutionResponseDTO>> getExecutionsByAgentId(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestExecution> executionPage = executionService.getExecutionsByAgentId(agentId, page, size);
        return Result.success(PageResult.from(executionPage.map(DTOConverter::toExecutionResponseDTO)));
    }

    @RequiresPermission("test:view")
    @Operation(summary = "根据测试用例ID获取执行列表")
    @GetMapping("/test-case/{testCaseId}")
    public Result<PageResult<ExecutionResponseDTO>> getExecutionsByTestCaseId(
            @PathVariable Long testCaseId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestExecution> executionPage = executionService.getExecutionsByTestCaseId(testCaseId, page, size);
        return Result.success(PageResult.from(executionPage.map(DTOConverter::toExecutionResponseDTO)));
    }

    @RequiresPermission("test:view")
    @Operation(summary = "根据状态获取测试执行列表")
    @GetMapping("/status/{tenantId}/{status}")
    public Result<PageResult<ExecutionResponseDTO>> getExecutionsByStatus(
            @PathVariable Long tenantId,
            @PathVariable Integer status,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestExecution> executionPage = executionService.getExecutionsByStatus(tenantId, status, page, size);
        return Result.success(PageResult.from(executionPage.map(DTOConverter::toExecutionResponseDTO)));
    }

    @RequiresPermission("test:view")
    @Operation(summary = "根据类型获取测试执行列表")
    @GetMapping("/type/{tenantId}/{executionType}")
    public Result<PageResult<ExecutionResponseDTO>> getExecutionsByType(
            @PathVariable Long tenantId,
            @PathVariable String executionType,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestExecution> executionPage = executionService.getExecutionsByType(tenantId, executionType, page, size);
        return Result.success(PageResult.from(executionPage.map(DTOConverter::toExecutionResponseDTO)));
    }

    @RequiresPermission("test:execute")
    @Operation(summary = "取消测试执行")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelExecution(@PathVariable Long id) {
        executionService.cancelExecution(id);
        return Result.success();
    }

    @RequiresPermission("test:view")
    @Operation(summary = "统计租户下测试执行数量")
    @GetMapping("/count/tenant/{tenantId}")
    public Result<Long> countExecutionsByTenant(@PathVariable Long tenantId) {
        long count = executionService.countExecutionsByTenant(tenantId);
        return Result.success(count);
    }

    @RequiresPermission("test:view")
    @Operation(summary = "统计Agent下测试执行数量")
    @GetMapping("/count/agent/{agentId}")
    public Result<Long> countExecutionsByAgent(@PathVariable Long agentId) {
        long count = executionService.countExecutionsByAgent(agentId);
        return Result.success(count);
    }

    @RequiresPermission("test:view")
    @Operation(summary = "统计测试用例下测试执行数量")
    @GetMapping("/count/test-case/{testCaseId}")
    public Result<Long> countExecutionsByTestCase(@PathVariable Long testCaseId) {
        long count = executionService.countExecutionsByTestCase(testCaseId);
        return Result.success(count);
    }
}
