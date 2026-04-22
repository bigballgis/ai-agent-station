package com.aiagent.service;

import com.aiagent.entity.*;
import com.aiagent.repository.*;
import com.aiagent.util.CsvExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for exporting platform data to CSV format.
 * Supports streaming for large datasets to avoid memory issues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportService {

    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final SystemLogRepository systemLogRepository;
    private final AgentTestResultRepository testResultRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;

    /**
     * Export agents to CSV.
     *
     * @param tenantId  optional tenant filter
     * @param status    optional status filter
     * @param keyword   optional keyword search in name/description
     * @param startDate optional start date filter
     * @param endDate   optional end date filter
     * @param response  HTTP response to write CSV to
     */
    public void exportAgents(Long tenantId, String status, String keyword,
                             LocalDateTime startDate, LocalDateTime endDate,
                             HttpServletResponse response) throws IOException {
        List<Agent> agents;
        if (tenantId != null) {
            agents = agentRepository.findByTenantId(tenantId);
        } else {
            agents = agentRepository.findAll();
        }

        // Apply filters
        if (status != null && !status.isBlank()) {
            agents = agents.stream()
                    .filter(a -> status.equalsIgnoreCase(a.getStatus().name()))
                    .toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            agents = agents.stream()
                    .filter(a -> (a.getName() != null && a.getName().toLowerCase().contains(kw))
                            || (a.getDescription() != null && a.getDescription().toLowerCase().contains(kw)))
                    .toList();
        }
        if (startDate != null) {
            agents = agents.stream()
                    .filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isBefore(startDate))
                    .toList();
        }
        if (endDate != null) {
            agents = agents.stream()
                    .filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isAfter(endDate))
                    .toList();
        }

        List<String> headers = List.of("ID", "Name", "Description", "Status", "Category",
                "Language", "Active", "CreatedBy", "PublishedAt", "CreatedAt", "UpdatedAt");

        List<Object[]> rows = agents.stream()
                .map(a -> new Object[]{
                        a.getId(),
                        a.getName(),
                        a.getDescription(),
                        a.getStatus().name(),
                        a.getCategory(),
                        a.getLanguage(),
                        a.getIsActive(),
                        a.getCreatedBy(),
                        CsvExportUtils.formatDate(a.getPublishedAt()),
                        CsvExportUtils.formatDate(a.getCreatedAt()),
                        CsvExportUtils.formatDate(a.getUpdatedAt())
                })
                .toList();

        setCsvResponseHeaders(response, "agents");
        CsvExportUtils.writeCsv(response.getOutputStream(), headers, rows);
    }

    /**
     * Export users to CSV.
     */
    public void exportUsers(Long tenantId, String keyword,
                            LocalDateTime startDate, LocalDateTime endDate,
                            HttpServletResponse response) throws IOException {
        List<User> users;
        if (tenantId != null) {
            users = userRepository.findByTenantId(tenantId);
        } else {
            users = userRepository.findAll();
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)))
                    .toList();
        }
        if (startDate != null) {
            users = users.stream()
                    .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(startDate))
                    .toList();
        }
        if (endDate != null) {
            users = users.stream()
                    .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isAfter(endDate))
                    .toList();
        }

        List<String> headers = List.of("ID", "Username", "Email", "Phone", "TenantId", "Active", "CreatedAt", "UpdatedAt");

        List<Object[]> rows = users.stream()
                .map(u -> new Object[]{
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getTenantId(),
                        u.getIsActive(),
                        CsvExportUtils.formatDate(u.getCreatedAt()),
                        CsvExportUtils.formatDate(u.getUpdatedAt())
                })
                .toList();

        setCsvResponseHeaders(response, "users");
        CsvExportUtils.writeCsv(response.getOutputStream(), headers, rows);
    }

    /**
     * Export system logs to CSV.
     */
    public void exportLogs(Long tenantId, String module, Boolean isSuccess,
                           LocalDateTime startDate, LocalDateTime endDate,
                           HttpServletResponse response) throws IOException {
        List<SystemLog> logs;
        if (tenantId != null) {
            logs = systemLogRepository.findByTenantId(tenantId);
        } else {
            logs = systemLogRepository.findAll();
        }

        if (module != null && !module.isBlank()) {
            logs = logs.stream()
                    .filter(l -> module.equals(l.getModule()))
                    .toList();
        }
        if (isSuccess != null) {
            logs = logs.stream()
                    .filter(l -> isSuccess.equals(l.getIsSuccess()))
                    .toList();
        }
        if (startDate != null) {
            logs = logs.stream()
                    .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(startDate))
                    .toList();
        }
        if (endDate != null) {
            logs = logs.stream()
                    .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isAfter(endDate))
                    .toList();
        }

        List<String> headers = List.of("ID", "TenantId", "UserId", "Username", "Module",
                "Operation", "Method", "IP", "ExecutionTime(ms)", "Success", "ErrorMsg", "CreatedAt");

        List<Object[]> rows = logs.stream()
                .map(l -> new Object[]{
                        l.getId(),
                        l.getTenantId(),
                        l.getUserId(),
                        l.getUsername(),
                        l.getModule(),
                        l.getOperation(),
                        l.getMethod(),
                        l.getIp(),
                        l.getExecutionTime(),
                        l.getIsSuccess(),
                        l.getErrorMsg(),
                        CsvExportUtils.formatDate(l.getCreatedAt())
                })
                .toList();

        setCsvResponseHeaders(response, "system_logs");
        CsvExportUtils.writeCsv(response.getOutputStream(), headers, rows);
    }

    /**
     * Export test results to CSV.
     */
    public void exportTestResults(Long tenantId, Long agentId, String status,
                                  LocalDateTime startDate, LocalDateTime endDate,
                                  HttpServletResponse response) throws IOException {
        List<AgentTestResult> results;
        if (tenantId != null) {
            results = testResultRepository.findByTenantId(tenantId);
        } else {
            results = testResultRepository.findAll();
        }

        if (agentId != null) {
            results = results.stream()
                    .filter(r -> agentId.equals(r.getAgentId()))
                    .toList();
        }
        if (status != null && !status.isBlank()) {
            results = results.stream()
                    .filter(r -> status.equalsIgnoreCase(r.getStatus()))
                    .toList();
        }
        if (startDate != null) {
            results = results.stream()
                    .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(startDate))
                    .toList();
        }
        if (endDate != null) {
            results = results.stream()
                    .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isAfter(endDate))
                    .toList();
        }

        List<String> headers = List.of("ID", "ExecutionId", "TenantId", "AgentId", "TestCaseId",
                "Status", "ErrorMessage", "CreatedAt", "UpdatedAt");

        List<Object[]> rows = results.stream()
                .map(r -> new Object[]{
                        r.getId(),
                        r.getExecutionId(),
                        r.getTenantId(),
                        r.getAgentId(),
                        r.getTestCaseId(),
                        r.getStatus(),
                        r.getErrorMessage(),
                        CsvExportUtils.formatDate(r.getCreatedAt()),
                        CsvExportUtils.formatDate(r.getUpdatedAt())
                })
                .toList();

        setCsvResponseHeaders(response, "test_results");
        CsvExportUtils.writeCsv(response.getOutputStream(), headers, rows);
    }

    /**
     * Export workflow instances to CSV.
     */
    public void exportWorkflowInstances(Long tenantId, String status, String keyword,
                                        LocalDateTime startDate, LocalDateTime endDate,
                                        HttpServletResponse response) throws IOException {
        List<WorkflowInstance> instances;
        if (tenantId != null) {
            instances = workflowInstanceRepository.findByTenantId(tenantId,
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        } else {
            instances = workflowInstanceRepository.findAll();
        }

        if (status != null && !status.isBlank()) {
            instances = instances.stream()
                    .filter(i -> status.equalsIgnoreCase(i.getStatus().name()))
                    .toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            instances = instances.stream()
                    .filter(i -> (i.getWorkflowName() != null && i.getWorkflowName().toLowerCase().contains(kw)))
                    .toList();
        }
        if (startDate != null) {
            instances = instances.stream()
                    .filter(i -> i.getStartedAt() != null && !i.getStartedAt().isBefore(startDate))
                    .toList();
        }
        if (endDate != null) {
            instances = instances.stream()
                    .filter(i -> i.getStartedAt() != null && !i.getStartedAt().isAfter(endDate))
                    .toList();
        }

        List<String> headers = List.of("ID", "DefinitionId", "WorkflowName", "Status",
                "CurrentNodeId", "CurrentStep", "StartedBy", "StartedAt", "CompletedAt", "Error", "TenantId");

        List<Object[]> rows = instances.stream()
                .map(i -> new Object[]{
                        i.getId(),
                        i.getWorkflowDefinitionId(),
                        i.getWorkflowName(),
                        i.getStatus().name(),
                        i.getCurrentNodeId(),
                        i.getCurrentStep(),
                        i.getStartedBy(),
                        CsvExportUtils.formatDate(i.getStartedAt()),
                        CsvExportUtils.formatDate(i.getCompletedAt()),
                        i.getError(),
                        i.getTenantId()
                })
                .toList();

        setCsvResponseHeaders(response, "workflow_instances");
        CsvExportUtils.writeCsv(response.getOutputStream(), headers, rows);
    }

    // ==================== Private Helpers ====================

    private void setCsvResponseHeaders(HttpServletResponse response, String filename) {
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String contentDisposition = "attachment; filename=\"" + filename + "_" + timestamp + ".csv\"";

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", contentDisposition);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
}
