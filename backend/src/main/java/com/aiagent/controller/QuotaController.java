package com.aiagent.controller;

import com.aiagent.annotation.RequiresRole;

import com.aiagent.common.Result;
import com.aiagent.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/v1/quotas")
@RequiredArgsConstructor
@Tag(name = "配额管理", description = "配额管理接口")
public class QuotaController {

    private final QuotaService quotaService;

    /**
     * Get tenant quota usage summary.
     *
     * @param tenantId the tenant ID from path
     * @param headerTenantId the tenant ID from header for isolation
     * @return quota usage summary
     */
    @RequiresRole("ADMIN")
    @GetMapping("/tenant/{tenantId}")
    public Result<?> getTenantQuota(
            @PathVariable String tenantId,
            @RequestHeader("X-Tenant-ID") String headerTenantId) {
        log.info("Get tenant quota: tenantId={}, headerTenantId={}", tenantId, headerTenantId);
        if (!tenantId.equals(headerTenantId)) {
            log.warn("Tenant ID mismatch: path={}, header={}", tenantId, headerTenantId);
            return Result.fail("Tenant ID mismatch");
        }
        try {
            return Result.success(quotaService.getTenantQuota(tenantId));
        } catch (Exception e) {
            log.error("Failed to get quota for tenant: {}", tenantId, e);
            return Result.fail("Failed to get quota: " + e.getMessage());
        }
    }

    /**
     * Get detailed tenant quota breakdown.
     *
     * @param tenantId the tenant ID from path
     * @param headerTenantId the tenant ID from header for isolation
     * @return detailed quota breakdown
     */
    @GetMapping("/tenant/{tenantId}/details")
    @Operation(summary = "获取租户配额概要")
    public Result<?> getTenantQuotaDetails(
            @PathVariable String tenantId,
            @RequestHeader("X-Tenant-ID") String headerTenantId) {
        log.info("Get tenant quota details: tenantId={}, headerTenantId={}", tenantId, headerTenantId);
        if (!tenantId.equals(headerTenantId)) {
            log.warn("Tenant ID mismatch: path={}, header={}", tenantId, headerTenantId);
            return Result.fail("Tenant ID mismatch");
        }
        try {
            return Result.success(quotaService.getTenantQuotaDetails(tenantId));
        } catch (Exception e) {
            log.error("Failed to get quota details for tenant: {}", tenantId, e);
            return Result.fail("Failed to get quota details: " + e.getMessage());
        }
    }

    /**
     * Update tenant quota limits.
     *
     * @param tenantId the tenant ID from path
     * @param headerTenantId the tenant ID from header for isolation
     * @param quotaUpdate the quota update request body
     * @return updated quota information
     */
    @PutMapping("/tenant/{tenantId}")
    @Operation(summary = "获取租户配额详情")
    public Result<?> updateTenantQuota(
            @PathVariable String tenantId,
            @RequestHeader("X-Tenant-ID") String headerTenantId,
            @RequestBody Object quotaUpdate) {
        log.info("Update tenant quota: tenantId={}, headerTenantId={}", tenantId, headerTenantId);
        if (!tenantId.equals(headerTenantId)) {
            log.warn("Tenant ID mismatch: path={}, header={}", tenantId, headerTenantId);
            return Result.fail("Tenant ID mismatch");
        }
        try {
            return Result.success(quotaService.updateTenantQuota(tenantId, quotaUpdate));
        } catch (Exception e) {
            log.error("Failed to update quota for tenant: {}", tenantId, e);
            return Result.fail("Failed to update quota: " + e.getMessage());
        }
    }
}
