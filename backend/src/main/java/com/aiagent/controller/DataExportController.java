package com.aiagent.controller;

import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;

import com.aiagent.service.DataExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for data export endpoints.
 * All endpoints return CSV file downloads with appropriate filter parameters.
 */
@RestController
@RequestMapping("/v1/export")
@RequiredArgsConstructor
@Tag(name = "数据导出", description = "数据导出接口")
public class DataExportController {

    private final DataExportService dataExportService;

    /**
     * Export agents as CSV.
     *
     * @param tenantId  filter by tenant ID
     * @param status    filter by agent status (DRAFT/PENDING_APPROVAL/APPROVED/PUBLISHED/ARCHIVED)
     * @param keyword   search in name and description
     * @param startDate filter by creation date (start)
     * @param endDate   filter by creation date (end)
     */
    @RequiresPermission("data:export")
    @GetMapping("/agents")
    @Operation(summary = "导出Agent数据为CSV")
    @OperationLog(value = "导出Agent数据", module = "数据导出")
    public void exportAgents(
            @RequestParam(required = false) @Parameter(description = "租户ID") Long tenantId,
            @RequestParam(required = false) @Parameter(description = "状态筛选") String status,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "开始时间") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "结束时间") LocalDateTime endDate,
            HttpServletResponse response) throws IOException {
        dataExportService.exportAgents(tenantId, status, keyword, startDate, endDate, response);
    }

    /**
     * Export users as CSV.
     *
     * @param tenantId  filter by tenant ID
     * @param keyword   search in username and email
     * @param startDate filter by creation date (start)
     * @param endDate   filter by creation date (end)
     */
    @RequiresPermission("data:export")
    @GetMapping("/users")
    @Operation(summary = "导出用户数据为CSV")
    @OperationLog(value = "导出用户数据", module = "数据导出")
    public void exportUsers(
            @RequestParam(required = false) @Parameter(description = "租户ID") Long tenantId,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "开始时间") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "结束时间") LocalDateTime endDate,
            HttpServletResponse response) throws IOException {
        dataExportService.exportUsers(tenantId, keyword, startDate, endDate, response);
    }

    /**
     * Export system logs as CSV.
     *
     * @param tenantId  filter by tenant ID
     * @param module    filter by module name
     * @param isSuccess filter by success status
     * @param startDate filter by creation date (start)
     * @param endDate   filter by creation date (end)
     */
    @RequiresPermission("data:export")
    @GetMapping("/logs")
    @Operation(summary = "导出系统日志为CSV")
    @OperationLog(value = "导出系统日志", module = "数据导出")
    public void exportLogs(
            @RequestParam(required = false) @Parameter(description = "租户ID") Long tenantId,
            @RequestParam(required = false) @Parameter(description = "模块名称") String module,
            @RequestParam(required = false) @Parameter(description = "是否成功") Boolean isSuccess,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "开始时间") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "结束时间") LocalDateTime endDate,
            HttpServletResponse response) throws IOException {
        dataExportService.exportLogs(tenantId, module, isSuccess, startDate, endDate, response);
    }

    /**
     * Export test results as CSV.
     *
     * @param tenantId  filter by tenant ID
     * @param agentId   filter by agent ID
     * @param status    filter by test status
     * @param startDate filter by creation date (start)
     * @param endDate   filter by creation date (end)
     */
    @RequiresPermission("data:export")
    @GetMapping("/test-results")
    @Operation(summary = "导出测试结果为CSV")
    @OperationLog(value = "导出测试结果", module = "数据导出")
    public void exportTestResults(
            @RequestParam(required = false) @Parameter(description = "租户ID") Long tenantId,
            @RequestParam(required = false) @Parameter(description = "Agent ID") Long agentId,
            @RequestParam(required = false) @Parameter(description = "状态") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "开始时间") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "结束时间") LocalDateTime endDate,
            HttpServletResponse response) throws IOException {
        dataExportService.exportTestResults(tenantId, agentId, status, startDate, endDate, response);
    }

    /**
     * Export workflow instances as CSV.
     *
     * @param tenantId  filter by tenant ID
     * @param status    filter by workflow status
     * @param keyword   search in workflow name
     * @param startDate filter by start date (start)
     * @param endDate   filter by start date (end)
     */
    @RequiresPermission("data:export")
    @GetMapping("/workflow-instances")
    @Operation(summary = "导出工作流实例为CSV")
    @OperationLog(value = "导出工作流实例", module = "数据导出")
    public void exportWorkflowInstances(
            @RequestParam(required = false) @Parameter(description = "租户ID") Long tenantId,
            @RequestParam(required = false) @Parameter(description = "状态") String status,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "开始时间") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "结束时间") LocalDateTime endDate,
            HttpServletResponse response) throws IOException {
        dataExportService.exportWorkflowInstances(tenantId, status, keyword, startDate, endDate, response);
    }
}
