package com.aiagent.controller;

import com.aiagent.service.DataExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * REST controller for data export endpoints.
 * All endpoints return CSV file downloads with appropriate filter parameters.
 */
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
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
    @GetMapping("/agents")
    public void exportAgents(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
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
    @GetMapping("/users")
    public void exportUsers(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
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
    @GetMapping("/logs")
    public void exportLogs(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Boolean isSuccess,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
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
    @GetMapping("/test-results")
    public void exportTestResults(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
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
    @GetMapping("/workflow-instances")
    public void exportWorkflowInstances(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletResponse response) throws IOException {
        dataExportService.exportWorkflowInstances(tenantId, status, keyword, startDate, endDate, response);
    }
}
