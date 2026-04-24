package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.service.RateLimitDashboardService;
import com.aiagent.service.RateLimitService;
import com.aiagent.service.RateLimitService.RateLimitStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 速率限制信息端点
 *
 * 提供当前租户/IP 的速率限制状态查询。
 * 帮助前端和运维人员了解当前限流使用情况。
 */
@RestController
@RequestMapping("/v1/rate-limits")
@RequiredArgsConstructor
@Tag(name = "速率限制管理")
public class RateLimitController {

    private final RateLimitService rateLimitService;
    private final RateLimitDashboardService rateLimitDashboardService;

    /**
     * 获取所有端点类型的速率限制状态
     */
    @GetMapping
    @Operation(summary = "获取所有端点类型的速率限制状态")
    public Result<Map<String, Object>> getAllRateLimitStatuses(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        List<RateLimitStatus> statuses = rateLimitService.getAllRateLimitStatuses(clientIp);

        Map<String, Object> data = new HashMap<>();
        data.put("statuses", statuses);
        data.put("clientIp", clientIp);

        return Result.success(data);
    }

    /**
     * 获取指定端点类型的速率限制状态
     */
    @GetMapping("/{endpointType}")
    @Operation(summary = "获取指定端点类型的速率限制状态")
    public Result<RateLimitStatus> getRateLimitStatus(
            @PathVariable String endpointType,
            HttpServletRequest request) {
        String clientIp = getClientIp(request);
        RateLimitStatus status = rateLimitService.getRateLimitStatus(endpointType.toUpperCase(), clientIp);
        return Result.success(status);
    }

    /**
     * 获取速率限制仪表盘统计数据
     * 包含: 当前窗口总请求数、限流命中数、Top 限流端点、各租户用量
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取速率限制仪表盘统计", description = "包含当前窗口总请求数、限流命中数、Top限流端点、各租户用量")
    public Result<Map<String, Object>> getRateLimitDashboard() {
        Map<String, Object> stats = rateLimitDashboardService.getRateLimitDashboardStats();
        return Result.success(stats);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
