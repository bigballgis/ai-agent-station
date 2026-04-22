package com.aiagent.controller;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.ApiInterface;
import com.aiagent.service.ApiInterfaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api-interfaces")
@RequiredArgsConstructor
public class ApiInterfaceController {

    private final ApiInterfaceService apiInterfaceService;

    @GetMapping
    public Result<PageResult<ApiInterface>> list(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiInterface> apiPage = apiInterfaceService.listByTenant(tenantId, pageable);
        return Result.success(PageResult.from(apiPage));
    }

    @GetMapping("/{id}")
    public Result<ApiInterface> getById(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        return Result.success(apiInterfaceService.getById(id, tenantId));
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<ApiInterface>> listByAgent(
            @PathVariable Long agentId,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        return Result.success(apiInterfaceService.listByAgent(agentId, tenantId));
    }

    @PostMapping
    public Result<ApiInterface> create(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestBody ApiInterface apiInterface) {
        apiInterface.setTenantId(tenantId);
        return Result.success(apiInterfaceService.create(apiInterface));
    }

    @PutMapping("/{id}")
    public Result<ApiInterface> update(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestBody ApiInterface apiInterface) {
        return Result.success(apiInterfaceService.update(id, tenantId, apiInterface));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        apiInterfaceService.delete(id, tenantId);
        return Result.success();
    }

    @PatchMapping("/{id}/toggle")
    public Result<ApiInterface> toggleActive(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("isActive");
        return Result.success(apiInterfaceService.toggleActive(id, tenantId, isActive));
    }
}
