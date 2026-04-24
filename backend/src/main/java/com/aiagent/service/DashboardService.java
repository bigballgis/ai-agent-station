package com.aiagent.service;

import com.aiagent.repository.*;
import com.aiagent.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AgentRepository agentRepository;
    private final WorkflowDefinitionRepository workflowRepository;
    private final ApiCallLogRepository apiCallLogRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final UserRepository userRepository;
    private final LoginLogRepository loginLogRepository;

    private final Map<String, HealthIndicator> healthIndicators;

    /**
     * 获取仪表盘统计数据，缓存5分钟
     */
    @Cacheable(value = "dashboardStats", key = "T(com.aiagent.util.SecurityUtils).getCurrentTenantId()")
    public Map<String, Object> getDashboardStats() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7).toLocalDate().atStartOfDay();
        LocalDateTime monthStart = now.minusDays(30).toLocalDate().atStartOfDay();

        Map<String, Object> stats = new LinkedHashMap<>();

        // 基础统计
        stats.put("totalAgents", agentRepository.count());
        stats.put("totalWorkflows", workflowRepository.count());
        stats.put("totalUsers", userRepository.count());

        // API 调用统计
        long apiCallsToday = countApiCalls(tenantId, todayStart);
        long apiCallsWeek = countApiCalls(tenantId, weekStart);
        long apiCallsMonth = countApiCalls(tenantId, monthStart);
        stats.put("apiCallsToday", apiCallsToday);
        stats.put("apiCallsWeek", apiCallsWeek);
        stats.put("apiCallsMonth", apiCallsMonth);

        // API 调用成功率（近7天）
        double successRate = calculateSuccessRate(tenantId, weekStart);
        stats.put("apiCallSuccessRate", Math.round(successRate * 100.0) / 100.0);

        // 活跃用户数（今日有登录记录的用户）
        long activeUsers = countActiveUsersToday();
        stats.put("activeUsersToday", activeUsers);

        // 近期告警数
        long recentAlerts = countRecentAlerts(tenantId, 24);
        stats.put("recentAlerts24h", recentAlerts);

        // Top 5 最常用 Agent
        stats.put("topAgents", getTopAgents(tenantId, weekStart, 5));

        // 系统健康状态
        stats.put("systemHealth", getSystemHealth());

        return stats;
    }

    private long countApiCalls(Long tenantId, LocalDateTime startTime) {
        try {
            Long count = apiCallLogRepository.countByTenantIdAndCreatedAtAfter(tenantId, startTime);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("统计API调用数失败: {}", e.getMessage());
            return 0L;
        }
    }

    private double calculateSuccessRate(Long tenantId, LocalDateTime startTime) {
        try {
            List<Object[]> statusCounts = apiCallLogRepository.countByStatusByTenantIdAndCreatedAtAfter(tenantId, startTime);
            long total = 0;
            long success = 0;
            for (Object[] row : statusCounts) {
                String status = String.valueOf(row[0]);
                long count = ((Number) row[1]).longValue();
                total += count;
                if ("SUCCESS".equalsIgnoreCase(status)) {
                    success += count;
                }
            }
            return total > 0 ? (double) success / total * 100 : 100.0;
        } catch (Exception e) {
            log.warn("计算API调用成功率失败: {}", e.getMessage());
            return 100.0;
        }
    }

    private long countActiveUsersToday() {
        try {
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            List<com.aiagent.entity.LoginLog> recentLogins = loginLogRepository.findByLoginTimeBetween(todayStart, LocalDateTime.now());
            return recentLogins.stream()
                    .map(com.aiagent.entity.LoginLog::getUserId)
                    .distinct()
                    .count();
        } catch (Exception e) {
            log.warn("统计活跃用户数失败: {}", e.getMessage());
            return 0L;
        }
    }

    private long countRecentAlerts(Long tenantId, int hours) {
        try {
            return alertRecordRepository.countByTenantIdAndStatusAndFiredAtAfter(
                    tenantId, "firing", LocalDateTime.now().minusHours(hours));
        } catch (Exception e) {
            log.warn("统计近期告警数失败: {}", e.getMessage());
            return 0L;
        }
    }

    private List<Map<String, Object>> getTopAgents(Long tenantId, LocalDateTime startTime, int limit) {
        try {
            List<Object[]> topAgentRows = apiCallLogRepository.findTopAgentsByCallCount(
                    tenantId, startTime, PageRequest.of(0, limit));
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : topAgentRows) {
                Long agentId = ((Number) row[0]).longValue();
                Long callCount = ((Number) row[1]).longValue();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("agentId", agentId);
                item.put("callCount", callCount);
                agentRepository.findById(agentId).ifPresent(agent -> {
                    item.put("agentName", agent.getName());
                });
                result.add(item);
            }
            return result;
        } catch (Exception e) {
            log.warn("获取Top Agent失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();

        // 数据库健康
        HealthIndicator dbHealth = healthIndicators.get("databaseConnectionHealth");
        if (dbHealth != null) {
            Health db = dbHealth.health();
            health.put("database", Map.of(
                    "status", db.getStatus().getCode(),
                    "details", db.getDetails()
            ));
        }

        // Redis 健康
        HealthIndicator redisHealth = healthIndicators.get("redisConnectionHealth");
        if (redisHealth != null) {
            Health redis = redisHealth.health();
            health.put("redis", Map.of(
                    "status", redis.getStatus().getCode(),
                    "details", redis.getDetails()
            ));
        }

        // 磁盘健康
        HealthIndicator diskHealth = healthIndicators.get("diskSpaceHealth");
        if (diskHealth != null) {
            Health disk = diskHealth.health();
            health.put("disk", Map.of(
                    "status", disk.getStatus().getCode(),
                    "details", disk.getDetails()
            ));
        }

        return health;
    }
}
