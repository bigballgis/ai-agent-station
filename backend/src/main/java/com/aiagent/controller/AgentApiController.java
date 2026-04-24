package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.Result;
import com.aiagent.common.ResultCode;
import com.aiagent.dto.AgentInvokeRequest;
import com.aiagent.dto.AgentInvokeResponse;
import com.aiagent.engine.AgentExecutionEngine;
import com.aiagent.entity.Agent;
import com.aiagent.entity.ApiCallLog;
import com.aiagent.exception.RateLimitException;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AgentService;
import com.aiagent.service.ApiCallLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.aiagent.service.AgentExecutionMonitor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Agent API", description = "Agent调用和管理接口")
public class AgentApiController {

    private final AgentExecutionEngine agentExecutionEngine;
    private final ApiCallLogService apiCallLogService;
    private final AgentService agentService;
    private final AgentExecutionMonitor executionMonitor;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/agent/{agentId}/invoke")
    @Operation(summary = "调用Agent", description = "同步或异步调用已发布的Agent")
    @RequiresPermission("agent:invoke")
    public Result<AgentInvokeResponse> invokeAgent(
            @Parameter(description = "Agent ID") @PathVariable Long agentId,
            @Valid @RequestBody AgentInvokeRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        // Agent调用限流：每用户每分钟最多30次
        String clientIp = getClientIp(httpRequest);
        checkRateLimit("agent-invoke:" + principal.getUserId() + ":" + clientIp, 30, 60);

        // 并发执行限制：每个 Agent 最多 N 个并发执行
        if (!executionMonitor.tryAcquireExecution(agentId)) {
            return Result.error(ResultCode.TOO_MANY_REQUESTS.getCode(), "Agent " + agentId + " 并发执行数已达上限，请稍后重试");
        }

        if (requestId == null || requestId.isEmpty()) {
            requestId = java.util.UUID.randomUUID().toString();
        }

        Long tenantId = principal.getTenantId();
        Long userId = principal.getUserId();

        AgentInvokeResponse response;
        try {
            long startTime = System.currentTimeMillis();
            response = agentExecutionEngine.invokeAgent(agentId, request, requestId, tenantId);
            int executionTime = (int) (System.currentTimeMillis() - startTime);
            response.setExecutionTime(executionTime);

            // 记录执行错误指标
            if ("FAILED".equals(response.getStatus())) {
                executionMonitor.recordError(agentId, response.getErrorMessage());
            }
        } finally {
            executionMonitor.releaseExecution(agentId);
        }

        // 记录API调用日志
        int statusCode = "SUCCESS".equals(response.getStatus()) || "ACCEPTED".equals(response.getStatus())
                ? 200 : 500;

        apiCallLogService.logApiCall(
                requestId, agentId, tenantId, userId, clientIp, "POST",
                "/api/v1/agent/" + agentId + "/invoke", null,
                request, response, statusCode, null,
                mapStatus(response.getStatus()), response.getExecutionTime(), request.getAsync(), response.getTaskId()
        );

        if ("SUCCESS".equals(response.getStatus()) || "ACCEPTED".equals(response.getStatus())) {
            return Result.success(response);
        }
        return Result.error(statusCode, response.getErrorMessage(), response);
    }

    @GetMapping("/agent/{agentId}/status")
    @Operation(summary = "获取Agent状态", description = "查询Agent的当前状态和发布信息")
    @RequiresPermission("agent:invoke")
    public Result<Map<String, Object>> getAgentStatus(
            @Parameter(description = "Agent ID") @PathVariable Long agentId) {
        Agent agent = agentService.findAgentById(agentId).orElse(null);
        if (agent == null) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND);
        }

        Map<String, Object> statusInfo = new HashMap<>();
        statusInfo.put("agentId", agent.getId());
        statusInfo.put("name", agent.getName());
        statusInfo.put("status", agent.getStatus().name());
        statusInfo.put("isActive", agent.getIsActive());
        statusInfo.put("category", agent.getCategory());

        if (agent.getPublishedAt() != null) {
            statusInfo.put("lastPublished", agent.getPublishedAt().toString());
        }

        if (agent.getPublishedVersionId() != null) {
            agentService.findVersionById(agent.getPublishedVersionId()).ifPresent(version -> {
                statusInfo.put("publishedVersionId", version.getId());
                statusInfo.put("publishedVersionNumber", version.getVersionNumber());
            });
        }

        return Result.success(statusInfo);
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "查询异步任务状态", description = "获取异步执行的Agent任务状态")
    @RequiresPermission("agent:invoke")
    public Result<AgentInvokeResponse> getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        AgentInvokeResponse response = agentExecutionEngine.getTaskStatus(taskId);
        if (response == null) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND);
        }
        return Result.success(response);
    }

    private ApiCallLog.ApiCallStatus mapStatus(String responseStatus) {
        return switch (responseStatus) {
            case "SUCCESS", "ACCEPTED" -> ApiCallLog.ApiCallStatus.SUCCESS;
            case "FAILED" -> ApiCallLog.ApiCallStatus.FAILED;
            case "RATE_LIMITED" -> ApiCallLog.ApiCallStatus.RATE_LIMITED;
            case "UNAUTHORIZED" -> ApiCallLog.ApiCallStatus.UNAUTHORIZED;
            default -> ApiCallLog.ApiCallStatus.FAILED;
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For may contain multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // ==================== 速率限制辅助方法 ====================

    private void checkRateLimit(String key, int maxAttempts, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
        if (count != null && count > maxAttempts) {
            throw new RateLimitException("请求过于频繁，请稍后重试");
        }
    }
}
