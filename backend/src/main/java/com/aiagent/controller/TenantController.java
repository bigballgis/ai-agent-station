package com.aiagent.controller;

import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.entity.Tenant;
import com.aiagent.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "租户管理", description = "租户管理接口")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @RequiresRole("SUPER_ADMIN")
    public Result<List<Tenant>> getAllTenants() {
        return Result.success(tenantService.getAllTenants());
    }

    @Operation(summary = "获取所有租户列表")
    @GetMapping("/{id}")
    @RequiresRole("SUPER_ADMIN")
    public Result<Tenant> getTenantById(@PathVariable Long id) {
        return Result.success(tenantService.getTenantById(id));
    }

    @Operation(summary = "根据ID获取租户详情")
    @PostMapping
    @RequiresRole("SUPER_ADMIN")
    public Result<Tenant> createTenant(@RequestBody Tenant tenant) {
        return Result.success(tenantService.createTenant(tenant));
    }

    @Operation(summary = "创建租户")
    @PutMapping("/{id}")
    @RequiresRole("SUPER_ADMIN")
    public Result<Tenant> updateTenant(@PathVariable Long id, @RequestBody Tenant tenant) {
        return Result.success(tenantService.updateTenant(id, tenant));
    }

    @Operation(summary = "更新租户")
    @DeleteMapping("/{id}")
    @RequiresRole("SUPER_ADMIN")
    public Result<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return Result.success();
    }

    @Operation(summary = "删除租户")
    @PostMapping("/{id}/regenerate-api-key")
    @RequiresRole("SUPER_ADMIN")
    public Result<Tenant> regenerateApiKey(@PathVariable Long id) {
        return Result.success(tenantService.regenerateApiKey(id));
    }
}
