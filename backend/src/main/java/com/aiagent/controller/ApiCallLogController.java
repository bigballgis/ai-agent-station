package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.ApiCallLog;
import com.aiagent.repository.ApiCallLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api-call-logs")
@RequiredArgsConstructor
@Tag(name = "API调用日志", description = "API调用日志查询与统计接口")
public class ApiCallLogController {

    private final ApiCallLogRepository apiCallLogRepository;

    @RequiresPermission("api:view")
    @GetMapping
    @Operation(summary = "分页查询API调用日志")
    public Result<PageResult<ApiCallLog>> list(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) Long apiInterfaceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String clientIp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ApiCallLog> logPage;

        if (agentId != null) {
            logPage = apiCallLogRepository.findByTenantIdAndAgentId(tenantId, agentId, pageable);
        } else if (apiInterfaceId != null) {
            logPage = apiCallLogRepository.findByTenantIdAndApiInterfaceId(tenantId, apiInterfaceId, pageable);
        } else {
            logPage = apiCallLogRepository.findByTenantId(tenantId, pageable);
        }

        return Result.success(PageResult.from(logPage));
    }

    @RequiresPermission("api:view")
    @GetMapping("/stats")
    @Operation(summary = "获取API调用统计信息")
    public Result<Map<String, Object>> getStats(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {

        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        Map<String, Object> stats = new HashMap<>();

        // 总调用次数
        Long totalCount;
        Double avgExecutionTime;
        Integer maxExecutionTime;

        if (agentId != null) {
            totalCount = apiCallLogRepository.countByTenantIdAndAgentIdAndCreatedAtAfter(tenantId, agentId, startTime);
            avgExecutionTime = apiCallLogRepository.findAverageExecutionTimeByTenantIdAndAgentIdAndCreatedAtAfter(tenantId, agentId, startTime);
            maxExecutionTime = apiCallLogRepository.findMaxExecutionTimeByTenantIdAndAgentIdAndCreatedAtAfter(tenantId, agentId, startTime);

            // Status distribution for specific agent
            List<Object[]> statusCounts = apiCallLogRepository.countByStatusByTenantIdAndAgentIdAndCreatedAtAfter(tenantId, agentId, startTime);
            Map<String, Long> statusMap = new HashMap<>();
            for (Object[] row : statusCounts) {
                statusMap.put(String.valueOf(row[0]), (Long) row[1]);
            }
            stats.put("statusDistribution", statusMap);
        } else {
            List<ApiCallLog> recentLogs = apiCallLogRepository.findByTenantIdAndCreatedAtBetween(tenantId, startTime, LocalDateTime.now());
            totalCount = (long) recentLogs.size();
            avgExecutionTime = recentLogs.stream()
                    .filter(l -> l.getExecutionTime() != null)
                    .mapToInt(ApiCallLog::getExecutionTime)
                    .average()
                    .orElse(0.0);
            maxExecutionTime = recentLogs.stream()
                    .filter(l -> l.getExecutionTime() != null)
                    .mapToInt(ApiCallLog::getExecutionTime)
                    .max()
                    .orElse(0);

            // Status distribution for tenant
            List<Object[]> statusCounts = apiCallLogRepository.countByStatusByTenantIdAndCreatedAtAfter(tenantId, startTime);
            Map<String, Long> statusMap = new HashMap<>();
            for (Object[] row : statusCounts) {
                statusMap.put(String.valueOf(row[0]), (Long) row[1]);
            }
            stats.put("statusDistribution", statusMap);
        }

        stats.put("totalCount", totalCount != null ? totalCount : 0);
        stats.put("avgExecutionTime", avgExecutionTime != null ? Math.round(avgExecutionTime) : 0);
        stats.put("maxExecutionTime", maxExecutionTime != null ? maxExecutionTime : 0);
        stats.put("periodDays", days);

        return Result.success(stats);
    }

    @RequiresPermission("api:view")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取API调用日志详情")
    public Result<ApiCallLog> getById(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        ApiCallLog log = apiCallLogRepository.findById(id)
                .filter(l -> l.getTenantId().equals(tenantId))
                .orElse(null);
        return Result.success(log);
    }

    @RequiresPermission("api:view")
    @GetMapping("/request/{requestId}")
    @Operation(summary = "根据请求ID获取API调用日志")
    public Result<ApiCallLog> getByRequestId(
            @PathVariable String requestId,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        ApiCallLog log = apiCallLogRepository.findByRequestId(requestId)
                .filter(l -> l.getTenantId().equals(tenantId))
                .orElse(null);
        return Result.success(log);
    }
}
