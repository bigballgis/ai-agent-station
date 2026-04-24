package com.aiagent.controller;

import com.aiagent.annotation.Audited;
import com.aiagent.annotation.AuditAction;
import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.service.DatabaseBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for database backup and recovery operations.
 */
@RestController
@RequestMapping("/v1/admin/backups")
@RequiredArgsConstructor
@Tag(name = "数据库备份管理", description = "数据库备份与恢复管理接口")
public class DatabaseBackupController {

    private final DatabaseBackupService databaseBackupService;

    @PostMapping
    @Operation(summary = "创建数据库备份", description = "使用pg_dump创建数据库备份")
    @OperationLog(value = "创建数据库备份", module = "数据库备份")
    @Audited(action = AuditAction.CREATE, module = "数据库备份", description = "创建数据库备份", resourceType = "Backup")
    @RequiresPermission("system:manage")
    @RequiresRole("SUPER_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "备份创建成功"),
            @ApiResponse(responseCode = "400", description = "备份功能已禁用"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "500", description = "备份失败")
    })
    public Result<Map<String, Object>> createBackup() {
        return databaseBackupService.createBackup();
    }

    @GetMapping
    @Operation(summary = "列出所有备份", description = "列出所有可用的数据库备份文件")
    @RequiresPermission("system:read")
    @RequiresRole("SUPER_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<List<Map<String, Object>>> listBackups() {
        return databaseBackupService.listBackups();
    }

    @PostMapping("/{backupId}/restore")
    @Operation(summary = "恢复数据库备份", description = "从指定备份恢复数据库（会自动创建恢复前备份）")
    @OperationLog(value = "恢复数据库备份", module = "数据库备份")
    @Audited(action = AuditAction.UPDATE, module = "数据库备份", description = "恢复数据库备份", resourceType = "Backup")
    @RequiresPermission("system:manage")
    @RequiresRole("SUPER_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "恢复成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "备份不存在"),
            @ApiResponse(responseCode = "500", description = "恢复失败")
    })
    public Result<Map<String, Object>> restoreBackup(
            @PathVariable @Parameter(description = "备份ID") String backupId) {
        return databaseBackupService.restoreBackup(backupId);
    }

    @PostMapping("/schedule")
    @Operation(summary = "设置自动备份计划", description = "使用cron表达式设置自动备份计划")
    @OperationLog(value = "设置自动备份计划", module = "数据库备份")
    @Audited(action = AuditAction.UPDATE, module = "数据库备份", description = "设置自动备份计划", resourceType = "Backup")
    @RequiresPermission("system:manage")
    @RequiresRole("SUPER_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "400", description = "无效的cron表达式"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Map<String, Object>> scheduleBackup(
            @RequestBody Map<String, String> request) {
        String cronExpression = request.get("cronExpression");
        if (cronExpression == null || cronExpression.isBlank()) {
            return Result.error(400, "cronExpression is required");
        }
        return databaseBackupService.scheduleBackup(cronExpression);
    }
}
