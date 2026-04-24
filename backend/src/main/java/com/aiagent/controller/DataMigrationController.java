package com.aiagent.controller;

import com.aiagent.annotation.Audited;
import com.aiagent.annotation.AuditAction;
import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.service.DataMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for data migration operations.
 * Provides tenant data export/import and migration status endpoints.
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "数据迁移管理", description = "数据迁移与导入导出管理接口")
public class DataMigrationController {

    private final DataMigrationService dataMigrationService;

    @PostMapping("/tenants/{id}/export")
    @Operation(summary = "导出租户数据", description = "导出指定租户的所有数据为JSON格式")
    @OperationLog(value = "导出租户数据", module = "数据迁移")
    @Audited(action = AuditAction.READ, module = "数据迁移", description = "导出租户数据", resourceType = "Tenant")
    @RequiresPermission("tenant:manage")
    @RequiresRole("SUPER_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "导出成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "租户不存在")
    })
    public Result<String> exportTenantData(
            @PathVariable @Parameter(description = "租户ID") Long id) {
        return dataMigrationService.exportTenantData(id);
    }

    @PostMapping(value = "/tenants/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "导入租户数据", description = "从JSON数据导入租户数据，将创建新租户")
    @OperationLog(value = "导入租户数据", module = "数据迁移")
    @Audited(action = AuditAction.CREATE, module = "数据迁移", description = "导入租户数据", resourceType = "Tenant")
    @RequiresPermission("tenant:manage")
    @RequiresRole("SUPER_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "导入成功"),
            @ApiResponse(responseCode = "400", description = "数据验证失败或JSON格式错误"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Map<String, Object>> importTenantData(
            @RequestBody String json) {
        return dataMigrationService.importTenantData(json);
    }

    @PostMapping(value = "/tenants/validate-import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "验证导入数据", description = "在导入前验证JSON数据的完整性和正确性")
    @RequiresPermission("tenant:manage")
    @RequiresRole("SUPER_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "验证通过"),
            @ApiResponse(responseCode = "400", description = "验证失败"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Map<String, Object>> validateImportData(
            @RequestBody String json) {
        return dataMigrationService.validateImportData(json);
    }

    @GetMapping("/migrations/status")
    @Operation(summary = "获取迁移状态", description = "获取Flyway数据库迁移状态信息")
    @RequiresPermission("system:read")
    @RequiresRole("SUPER_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Map<String, Object>> getMigrationStatus() {
        return dataMigrationService.getMigrationStatus();
    }
}
