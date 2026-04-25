package com.aiagent.mcp;

import com.aiagent.entity.McpTool;
import com.aiagent.repository.McpToolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * MCP 工具健康检查调度器
 *
 * 核心能力:
 * 1. 每 5 分钟对所有活跃 MCP 工具执行连通性检查（ping）
 * 2. 自动更新工具的 healthStatus / lastHealthCheck / consecutiveFailures
 * 3. 连续 3 次失败自动禁用工具并记录告警日志
 * 4. 工具恢复后自动重置失败计数并标记为 HEALTHY
 */
@Component
public class McpToolHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(McpToolHealthChecker.class);

    /** 连续失败多少次后自动禁用 */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    /** 健康检查超时（毫秒） */
    private static final int HEALTH_CHECK_TIMEOUT_MS = 10_000;

    private final McpToolRepository mcpToolRepository;
    private final RestTemplate healthCheckRestTemplate;

    /**
     * 记录上一次检查结果，用于检测状态变化并发出告警
     */
    private final ConcurrentHashMap<Long, String> previousStatusMap = new ConcurrentHashMap<>();

    public McpToolHealthChecker(McpToolRepository mcpToolRepository) {
        this.mcpToolRepository = mcpToolRepository;
        this.healthCheckRestTemplate = new RestTemplate();
        var factory = (org.springframework.http.client.SimpleClientHttpRequestFactory)
                this.healthCheckRestTemplate.getRequestFactory();
        factory.setConnectTimeout(HEALTH_CHECK_TIMEOUT_MS);
        factory.setReadTimeout(HEALTH_CHECK_TIMEOUT_MS);
    }

    /**
     * 定时健康检查：每 5 分钟执行一次
     * （避免在 Javadoc 中写 "星号+斜杠" 的 cron 片段，否则会被误解析为注释结束。）
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void checkAllToolsHealth() {
        log.info("[MCP HealthChecker] 开始执行健康检查...");
        List<McpTool> activeTools = mcpToolRepository.findByIsActiveTrue();

        int healthyCount = 0;
        int degradedCount = 0;
        int unhealthyCount = 0;

        for (McpTool tool : activeTools) {
            try {
                checkSingleToolHealth(tool);
                String status = tool.getHealthStatus();
                if ("HEALTHY".equals(status)) healthyCount++;
                else if ("DEGRADED".equals(status)) degradedCount++;
                else unhealthyCount++;
            } catch (Exception e) {
                log.error("[MCP HealthChecker] 检查工具异常: toolId={}, error={}",
                        tool.getId(), e.getMessage());
            }
        }

        log.info("[MCP HealthChecker] 健康检查完成: total={}, healthy={}, degraded={}, unhealthy={}",
                activeTools.size(), healthyCount, degradedCount, unhealthyCount);
    }

    /**
     * 对单个工具执行健康检查
     */
    private void checkSingleToolHealth(McpTool tool) {
        String serverUrl = resolveServerUrl(tool);
        if (serverUrl == null || serverUrl.isBlank()) {
            // 没有配置端点 URL，跳过检查
            tool.setHealthStatus("UNKNOWN");
            tool.setLastHealthCheck(LocalDateTime.now());
            mcpToolRepository.save(tool);
            return;
        }

        long startTime = System.currentTimeMillis();
        boolean success = false;
        String newStatus;

        try {
            // 发送轻量级 ping 请求（MCP initialize 或简单 HTTP HEAD）
            if ("MCP".equalsIgnoreCase(tool.getToolType())) {
                success = pingMcpServer(serverUrl);
            } else {
                success = pingHttpEndpoint(serverUrl);
            }
        } catch (ResourceAccessException e) {
            // 连接超时或网络不可达
            log.warn("[MCP HealthChecker] 连接失败: tool={}, url={}, error={}",
                    tool.getToolName(), serverUrl, e.getMessage());
            success = false;
        } catch (Exception e) {
            log.warn("[MCP HealthChecker] 检查异常: tool={}, error={}",
                    tool.getToolName(), e.getMessage());
            success = false;
        }

        long responseTime = System.currentTimeMillis() - startTime;

        if (success) {
            if (responseTime > 5000) {
                newStatus = "DEGRADED";
            } else {
                newStatus = "HEALTHY";
            }
            tool.setConsecutiveFailures(0);
        } else {
            int failures = (tool.getConsecutiveFailures() != null ? tool.getConsecutiveFailures() : 0) + 1;
            tool.setConsecutiveFailures(failures);

            if (failures >= MAX_CONSECUTIVE_FAILURES) {
                newStatus = "UNHEALTHY";
                tool.setIsActive(false);
                log.warn("[MCP HealthChecker] 工具已自动禁用: tool={}, consecutiveFailures={}",
                        tool.getToolName(), failures);
            } else {
                newStatus = "DEGRADED";
            }
        }

        // 更新平均响应时间（指数移动平均）
        long prevAvg = tool.getAvgResponseTime() != null ? tool.getAvgResponseTime() : 0;
        if (success && prevAvg > 0) {
            tool.setAvgResponseTime((prevAvg * 9 + responseTime) / 10);
        } else if (success) {
            tool.setAvgResponseTime(responseTime);
        }

        // 检测状态变化并告警
        String prevStatus = previousStatusMap.get(tool.getId());
        if (prevStatus != null && !prevStatus.equals(newStatus)) {
            log.warn("[MCP HealthChecker] 工具状态变化告警: tool={}, {} -> {}",
                    tool.getToolName(), prevStatus, newStatus);
        }

        tool.setHealthStatus(newStatus);
        tool.setLastHealthCheck(LocalDateTime.now());
        mcpToolRepository.save(tool);
        previousStatusMap.put(tool.getId(), newStatus);
    }

    /**
     * Ping MCP 服务器（发送 initialize 请求）
     */
    private boolean pingMcpServer(String serverUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = """
                    {
                        "jsonrpc": "2.0",
                        "id": 1,
                        "method": "initialize",
                        "params": {
                            "protocolVersion": "2024-11-05",
                            "clientInfo": {"name": "health-check", "version": "1.0.0"},
                            "capabilities": {}
                        }
                    }
                    """;
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = healthCheckRestTemplate.exchange(
                    serverUrl, HttpMethod.POST, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Ping HTTP 端点（发送 HEAD 请求，失败则回退 GET）
     */
    private boolean pingHttpEndpoint(String serverUrl) {
        try {
            ResponseEntity<String> response = healthCheckRestTemplate.exchange(
                    serverUrl, HttpMethod.HEAD, null, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 4xx/5xx 也说明服务可达
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析工具的服务端点 URL
     */
    private String resolveServerUrl(McpTool tool) {
        if (tool.getEndpointUrl() != null && !tool.getEndpointUrl().isBlank()) {
            return tool.getEndpointUrl();
        }
        if (tool.getConfig() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = om.readTree(tool.getConfig());
                if (node.has("serverUrl")) {
                    return node.get("serverUrl").asText();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * 手动触发单个工具的健康检查（供 Controller 调用）
     */
    public McpTool checkToolHealthNow(Long toolId) {
        return mcpToolRepository.findById(toolId).map(tool -> {
            checkSingleToolHealth(tool);
            return tool;
        }).orElse(null);
    }
}
