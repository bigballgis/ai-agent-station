package com.aiagent.controller;

import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.entity.Tenant;
import com.aiagent.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @RequiresRole("SUPER_ADMIN")
    public Result<List<Tenant>> getAllTenants() {
        return Result.success(tenantService.getAllTenants());
    }

    @GetMapping("/{id}")
    @RequiresRole("SUPER_ADMIN")
    public Result<Tenant> getTenantById(@PathVariable Long id) {
        return Result.success(tenantService.getTenantById(id));
    }

    @PostMapping
    @RequiresRole("SUPER_ADMIN")
    public Result<Tenant> createTenant(@RequestBody Tenant tenant) {
        return Result.success(tenantService.createTenant(tenant));
    }

    @PutMapping("/{id}")
    @RequiresRole("SUPER_ADMIN")
    public Result<Tenant> updateTenant(@PathVariable Long id, @RequestBody Tenant tenant) {
        return Result.success(tenantService.updateTenant(id, tenant));
    }

    @DeleteMapping("/{id}")
    @RequiresRole("SUPER_ADMIN")
    public Result<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return Result.success();
    }

    @PostMapping("/{id}/regenerate-api-key")
    @RequiresRole("SUPER_ADMIN")
    public Result<Tenant> regenerateApiKey(@PathVariable Long id) {
        return Result.success(tenantService.regenerateApiKey(id));
    }
}
