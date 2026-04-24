package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.dto.AgentInvokeRequest;
import com.aiagent.dto.AgentInvokeResponse;
import com.aiagent.engine.AgentExecutionEngine;
import com.aiagent.entity.Agent;
import com.aiagent.entity.ApiCallLog;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AgentService;
import com.aiagent.service.ApiCallLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Agent API", description = "Agent调用和管理接口")
public class AgentApiController {

    private final AgentExecutionEngine agentExecutionEngine;
    private final ApiCallLogService apiCallLogService;
    private final AgentService agentService;

    @PostMapping("/agent/{agentId}/invoke")
    @Operation(summary = "调用Agent", description = "同步或异步调用已发布的Agent")
    @RequiresPermission("agent:invoke")
    public ResponseEntity<AgentInvokeResponse> invokeAgent(
            @Parameter(description = "Agent ID") @PathVariable Long agentId,
            @Valid @RequestBody AgentInvokeRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        if (requestId == null || requestId.isEmpty()) {
            requestId = java.util.UUID.randomUUID().toString();
        }

        Long tenantId = principal.getTenantId();
        Long userId = principal.getUserId();

        long startTime = System.currentTimeMillis();
        AgentInvokeResponse response = agentExecutionEngine.invokeAgent(agentId, request, requestId, tenantId);
        int executionTime = (int) (System.currentTimeMillis() - startTime);
        response.setExecutionTime(executionTime);

        // 记录API调用日志
        HttpStatus status = "SUCCESS".equals(response.getStatus()) || "ACCEPTED".equals(response.getStatus())
                ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;

        String clientIp = getClientIp(httpRequest);

        apiCallLogService.logApiCall(
                requestId, agentId, tenantId, userId, clientIp, "POST",
                "/api/v1/agent/" + agentId + "/invoke", null,
                request, response, status.value(), null,
                mapStatus(response.getStatus()), executionTime, request.getAsync(), response.getTaskId()
        );

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/agent/{agentId}/status")
    @Operation(summary = "获取Agent状态", description = "查询Agent的当前状态和发布信息")
    @RequiresPermission("agent:invoke")
    public ResponseEntity<?> getAgentStatus(
            @Parameter(description = "Agent ID") @PathVariable Long agentId) {
        Agent agent = agentService.findAgentById(agentId).orElse(null);
        if (agent == null) {
            return ResponseEntity.notFound().build();
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

        return ResponseEntity.ok(statusInfo);
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "查询异步任务状态", description = "获取异步执行的Agent任务状态")
    @RequiresPermission("agent:invoke")
    public ResponseEntity<AgentInvokeResponse> getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        AgentInvokeResponse response = agentExecutionEngine.getTaskStatus(taskId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
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
}
