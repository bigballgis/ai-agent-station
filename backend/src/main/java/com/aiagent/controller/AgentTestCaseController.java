package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.CreateTestCaseRequestDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.TestCaseResponseDTO;
import com.aiagent.dto.UpdateTestCaseRequestDTO;
import com.aiagent.entity.AgentTestCase;
import com.aiagent.service.AgentTestCaseService;
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
@RequestMapping("/v1/test-cases")
@RequiredArgsConstructor
@Tag(name = "测试用例管理", description = "Agent测试用例管理接口")
public class AgentTestCaseController {

    private final AgentTestCaseService testCaseService;

    @RequiresPermission("test:manage")
    @PostMapping
    @Operation(summary = "创建测试用例")
    public Result<TestCaseResponseDTO> createTestCase(@Valid @RequestBody CreateTestCaseRequestDTO request) {
        AgentTestCase testCase = DTOConverter.toTestCaseEntity(request);
        AgentTestCase createdTestCase = testCaseService.createTestCase(testCase);
        return Result.success(DTOConverter.toTestCaseResponseDTO(createdTestCase));
    }

    @RequiresPermission("test:view")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取测试用例详情")
    public Result<TestCaseResponseDTO> getTestCaseById(@PathVariable Long id) {
        return testCaseService.getTestCaseById(id)
                .map(DTOConverter::toTestCaseResponseDTO)
                .map(Result::success)
                .orElse(Result.error("Test case not found"));
    }

    @RequiresPermission("test:view")
    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "根据租户ID获取测试用例列表")
    public Result<PageResult<TestCaseResponseDTO>> getTestCasesByTenantId(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestCase> testCasePage = testCaseService.getTestCasesByTenantId(tenantId, page, size);
        return Result.success(PageResult.from(testCasePage.map(DTOConverter::toTestCaseResponseDTO)));
    }

    @RequiresPermission("test:view")
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "根据Agent ID获取测试用例列表")
    public Result<PageResult<TestCaseResponseDTO>> getTestCasesByAgentId(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestCase> testCasePage = testCaseService.getTestCasesByAgentId(agentId, page, size);
        return Result.success(PageResult.from(testCasePage.map(DTOConverter::toTestCaseResponseDTO)));
    }

    @RequiresPermission("test:manage")
    @PutMapping("/{id}")
    @Operation(summary = "更新测试用例")
    public Result<TestCaseResponseDTO> updateTestCase(@PathVariable Long id, @Valid @RequestBody UpdateTestCaseRequestDTO request) {
        AgentTestCase testCase = testCaseService.getTestCaseById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));
        DTOConverter.updateTestCaseFromDTO(request, testCase);
        AgentTestCase updatedTestCase = testCaseService.updateTestCase(id, testCase);
        return Result.success(DTOConverter.toTestCaseResponseDTO(updatedTestCase));
    }

    @RequiresPermission("test:manage")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除测试用例")
    public Result<Void> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return Result.success();
    }

    @RequiresPermission("test:view")
    @GetMapping("/code/{tenantId}/{testCode}")
    @Operation(summary = "根据编码获取测试用例")
    public Result<TestCaseResponseDTO> getTestCaseByCode(@PathVariable Long tenantId, @PathVariable String testCode) {
        AgentTestCase testCase = testCaseService.getTestCaseByCode(tenantId, testCode);
        return testCase != null ? Result.success(DTOConverter.toTestCaseResponseDTO(testCase)) : Result.error("Test case not found");
    }

    @RequiresPermission("test:view")
    @GetMapping("/status/{tenantId}/{status}")
    @Operation(summary = "根据状态获取测试用例列表")
    public Result<PageResult<TestCaseResponseDTO>> getTestCasesByStatus(
            @PathVariable Long tenantId,
            @PathVariable Integer status,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestCase> testCasePage = testCaseService.getTestCasesByStatus(tenantId, status, page, size);
        return Result.success(PageResult.from(testCasePage.map(DTOConverter::toTestCaseResponseDTO)));
    }

    @RequiresPermission("test:view")
    @GetMapping("/type/{tenantId}/{testType}")
    @Operation(summary = "根据类型获取测试用例列表")
    public Result<PageResult<TestCaseResponseDTO>> getTestCasesByType(
            @PathVariable Long tenantId,
            @PathVariable String testType,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        Page<AgentTestCase> testCasePage = testCaseService.getTestCasesByType(tenantId, testType, page, size);
        return Result.success(PageResult.from(testCasePage.map(DTOConverter::toTestCaseResponseDTO)));
    }

    @RequiresPermission("test:view")
    @GetMapping("/count/tenant/{tenantId}")
    @Operation(summary = "统计租户下测试用例数量")
    public Result<Long> countTestCasesByTenant(@PathVariable Long tenantId) {
        long count = testCaseService.countTestCasesByTenant(tenantId);
        return Result.success(count);
    }

    @RequiresPermission("test:view")
    @GetMapping("/count/agent/{agentId}")
    @Operation(summary = "统计Agent下测试用例数量")
    public Result<Long> countTestCasesByAgent(@PathVariable Long agentId) {
        long count = testCaseService.countTestCasesByAgent(agentId);
        return Result.success(count);
    }
}
