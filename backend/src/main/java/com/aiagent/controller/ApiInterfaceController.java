package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.ApiInterface;
import com.aiagent.service.ApiInterfaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/api-interfaces")
@RequiredArgsConstructor
@Tag(name = "API接口管理", description = "API接口管理接口")
public class ApiInterfaceController {

    private final ApiInterfaceService apiInterfaceService;

    @RequiresPermission("api:view")
    @GetMapping
    @Operation(summary = "分页查询API接口列表")
    public Result<PageResult<ApiInterface>> list(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiInterface> apiPage = apiInterfaceService.listByTenant(tenantId, pageable);
        return Result.success(PageResult.from(apiPage));
    }

    @RequiresPermission("api:read")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取API接口详情")
    public Result<ApiInterface> getById(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        return Result.success(apiInterfaceService.getById(id, tenantId));
    }

    @RequiresPermission("api:read")
    @Operation(summary = "根据Agent ID获取API接口列表")
    @GetMapping("/agent/{agentId}")
    public Result<List<ApiInterface>> listByAgent(
            @PathVariable Long agentId,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        return Result.success(apiInterfaceService.listByAgent(agentId, tenantId));
    }

    @RequiresPermission("api:manage")
    @PostMapping
    @Operation(summary = "创建API接口")
    public Result<ApiInterface> create(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @Valid @RequestBody ApiInterface apiInterface) {
        apiInterface.setTenantId(tenantId);
        return Result.success(apiInterfaceService.create(apiInterface));
    }

    @RequiresPermission("api:write")
    @PutMapping("/{id}")
    @Operation(summary = "更新API接口")
    public Result<ApiInterface> update(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @Valid @RequestBody ApiInterface apiInterface) {
        return Result.success(apiInterfaceService.update(id, tenantId, apiInterface));
    }

    @RequiresPermission("api:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除API接口")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        apiInterfaceService.delete(id, tenantId);
        return Result.success();
    }

    @RequiresPermission("api:write")
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "切换API接口启用状态")
    public Result<ApiInterface> toggleActive(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("isActive");
        return Result.success(apiInterfaceService.toggleActive(id, tenantId, isActive));
    }
}
