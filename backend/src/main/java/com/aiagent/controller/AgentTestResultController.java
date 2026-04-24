package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.TestResultResponseDTO;
import com.aiagent.dto.UpdateTestResultRequestDTO;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.service.AgentTestResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
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
    public Result<TestResultResponseDTO> getResultById(@PathVariable Long id) {
        return resultService.getResultById(id)
                .map(DTOConverter::toTestResultResponseDTO)
                .map(Result::success)
                .orElse(Result.fail("Test result not found"));
    }

    @Operation(summary = "根据ID获取测试结果详情")
    @RequiresPermission("test:view")
    @GetMapping("/execution/{executionId}")
    public Result<PageResult<TestResultResponseDTO>> getResultsByExecutionId(
            @PathVariable Long executionId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        // TODO: 后续应改为数据库层面分页，避免内存中处理大量数据
        List<AgentTestResult> results = resultService.getResultsByExecutionId(executionId);
        List<TestResultResponseDTO> dtoList = results.stream()
                .map(DTOConverter::toTestResultResponseDTO)
                .collect(Collectors.toList());
        return Result.success(PageResult.paginate(dtoList, page, size));
    }

    @Operation(summary = "根据执行ID获取测试结果列表")
    @RequiresPermission("test:view")
    @GetMapping("/tenant/{tenantId}")
    public Result<PageResult<TestResultResponseDTO>> getResultsByTenantId(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        // TODO: 后续应改为数据库层面分页，避免内存中处理大量数据
        List<AgentTestResult> results = resultService.getResultsByTenantId(tenantId);
        List<TestResultResponseDTO> dtoList = results.stream()
                .map(DTOConverter::toTestResultResponseDTO)
                .collect(Collectors.toList());
        return Result.success(PageResult.paginate(dtoList, page, size));
    }

    @Operation(summary = "根据租户ID获取测试结果列表")
    @RequiresPermission("test:view")
    @GetMapping("/agent/{agentId}")
    public Result<PageResult<TestResultResponseDTO>> getResultsByAgentId(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        // TODO: 后续应改为数据库层面分页，避免内存中处理大量数据
        List<AgentTestResult> results = resultService.getResultsByAgentId(agentId);
        List<TestResultResponseDTO> dtoList = results.stream()
                .map(DTOConverter::toTestResultResponseDTO)
                .collect(Collectors.toList());
        return Result.success(PageResult.paginate(dtoList, page, size));
    }

    @Operation(summary = "根据Agent ID获取测试结果列表")
    @RequiresPermission("test:view")
    @GetMapping("/test-case/{testCaseId}")
    public Result<PageResult<TestResultResponseDTO>> getResultsByTestCaseId(
            @PathVariable Long testCaseId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        // TODO: 后续应改为数据库层面分页，避免内存中处理大量数据
        List<AgentTestResult> results = resultService.getResultsByTestCaseId(testCaseId);
        List<TestResultResponseDTO> dtoList = results.stream()
                .map(DTOConverter::toTestResultResponseDTO)
                .collect(Collectors.toList());
        return Result.success(PageResult.paginate(dtoList, page, size));
    }

    @Operation(summary = "根据状态获取测试结果列表")
    @RequiresPermission("test:view")
    @GetMapping("/status/{tenantId}/{status}")
    public Result<PageResult<TestResultResponseDTO>> getResultsByStatus(
            @PathVariable Long tenantId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        // TODO: 后续应改为数据库层面分页，避免内存中处理大量数据
        List<AgentTestResult> results = resultService.getResultsByStatus(tenantId, status);
        List<TestResultResponseDTO> dtoList = results.stream()
                .map(DTOConverter::toTestResultResponseDTO)
                .collect(Collectors.toList());
        return Result.success(PageResult.paginate(dtoList, page, size));
    }

    @Operation(summary = "根据状态获取测试结果列表")
    @RequiresPermission("test:manage")
    @PutMapping("/{id}")
    public Result<TestResultResponseDTO> updateResult(@PathVariable Long id, @Valid @RequestBody UpdateTestResultRequestDTO request) {
        AgentTestResult result = resultService.getResultById(id)
                .orElseThrow(() -> new RuntimeException("Test result not found"));
        DTOConverter.updateTestResultFromDTO(request, result);
        AgentTestResult updatedResult = resultService.updateResult(id, result);
        return Result.success(DTOConverter.toTestResultResponseDTO(updatedResult));
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
