package com.aiagent.controller;

import com.aiagent.common.result.Result;
import com.aiagent.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/quotas")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;

    /**
     * Get tenant quota usage summary.
     *
     * @param tenantId the tenant ID from path
     * @param headerTenantId the tenant ID from header for isolation
     * @return quota usage summary
     */
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
