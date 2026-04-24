package com.aiagent.service;

import com.aiagent.config.DataRetentionConfig;
import com.aiagent.entity.SystemLog;
import com.aiagent.repository.SystemLogRepository;
import com.aiagent.repository.AgentTestResultRepository;
import com.aiagent.repository.LoginLogRepository;
import com.aiagent.repository.TenantRepository;
import com.aiagent.repository.UserSessionRepository;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据保留策略服务
 * 管理各类数据的保留策略，定期清理过期数据
 * 支持租户隔离清理和审计日志记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionPolicyService {

    private final SystemLogRepository systemLogRepository;
    private final AgentTestResultRepository testResultRepository;
    private final LoginLogRepository loginLogRepository;
    private final UserSessionRepository userSessionRepository;
    private final TenantRepository tenantRepository;
    private final DataRetentionConfig retentionConfig;
    private final SystemLogService systemLogService;

    /**
     * 获取当前保留策略（从配置中读取）
     *
     * @return 保留策略配置
     */
    public Map<String, Object> getRetentionPolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("logs", Map.of("retentionDays", retentionConfig.getLogsDays(), "description", "系统日志保留天数"));
        policy.put("testResults", Map.of("retentionDays", retentionConfig.getTestResultsDays(), "description", "测试结果保留天数"));
        policy.put("auditLogs", Map.of("retentionDays", retentionConfig.getAuditLogsDays(), "description", "审计日志保留天数"));
        policy.put("loginLogs", Map.of("retentionDays", retentionConfig.getLoginLogsDays(), "description", "登录日志保留天数"));
        return policy;
    }

    /**
     * 执行保留策略清理（定时任务，每天凌晨3点执行）
     * 支持租户隔离清理，每个租户独立执行清理操作
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void executeRetentionCleanup() {
        log.info("开始执行数据保留策略清理...");

        LocalDateTime now = LocalDateTime.now();
        long totalDeleted = 0;

        // 1. 清理过期系统日志（全局清理，按租户隔离）
        LocalDateTime logsThreshold = now.minusDays(retentionConfig.getLogsDays());
        long logsCount = cleanupOldSystemLogs(logsThreshold);
        totalDeleted += logsCount;
        log.info("清理系统日志完成: 删除{}条超过{}天的记录", logsCount, retentionConfig.getLogsDays());

        // 2. 清理过期测试结果（全局清理，按租户隔离）
        LocalDateTime testResultsThreshold = now.minusDays(retentionConfig.getTestResultsDays());
        long testResultsCount = cleanupOldTestResults(testResultsThreshold);
        totalDeleted += testResultsCount;
        log.info("清理测试结果完成: 删除{}条超过{}天的记录", testResultsCount, retentionConfig.getTestResultsDays());

        // 3. 清理过期登录日志
        LocalDateTime loginLogsThreshold = now.minusDays(retentionConfig.getLoginLogsDays());
        long loginLogsCount = cleanupOldLoginLogs(loginLogsThreshold);
        totalDeleted += loginLogsCount;
        log.info("清理登录日志完成: 删除{}条超过{}天的记录", loginLogsCount, retentionConfig.getLoginLogsDays());

        // 4. 清理过期非活跃会话
        long sessionsCount = cleanupOldSessions(now.minusDays(30));
        totalDeleted += sessionsCount;
        log.info("清理过期会话完成: 删除{}条超过30天的非活跃会话", sessionsCount);

        log.info("数据保留策略清理全部完成, 共删除{}条记录", totalDeleted);

        // 记录审计日志
        recordCleanupAuditLog(totalDeleted, null);
    }

    /**
     * 执行指定租户的数据保留策略清理
     *
     * @param tenantId 租户ID
     * @return 清理的记录总数
     */
    @Transactional(rollbackFor = Exception.class)
    public long executeRetentionCleanupForTenant(Long tenantId) {
        log.info("开始执行租户[{}]数据保留策略清理...", tenantId);

        LocalDateTime now = LocalDateTime.now();
        long totalDeleted = 0;

        // 按租户清理系统日志
        LocalDateTime logsThreshold = now.minusDays(retentionConfig.getLogsDays());
        try {
            int count = systemLogRepository.deleteByTenantIdAndCreatedAtBefore(tenantId, logsThreshold);
            totalDeleted += count;
            log.info("租户[{}]清理系统日志完成: 删除{}条", tenantId, count);
        } catch (Exception e) {
            log.error("租户[{}]清理系统日志失败", tenantId, e);
        }

        // 按租户清理测试结果
        LocalDateTime testResultsThreshold = now.minusDays(retentionConfig.getTestResultsDays());
        try {
            int count = testResultRepository.deleteByTenantIdAndCreatedAtBefore(tenantId, testResultsThreshold);
            totalDeleted += count;
            log.info("租户[{}]清理测试结果完成: 删除{}条", tenantId, count);
        } catch (Exception e) {
            log.error("租户[{}]清理测试结果失败", tenantId, e);
        }

        log.info("租户[{}]数据保留策略清理完成, 共删除{}条记录", tenantId, totalDeleted);

        // 记录审计日志
        recordCleanupAuditLog(totalDeleted, tenantId);

        return totalDeleted;
    }

    /**
     * 更新保留策略
     *
     * @param policy 新的保留策略
     * @return 更新后的保留策略
     */
    public Map<String, Object> updateRetentionPolicy(Map<String, Object> policy) {
        if (policy.containsKey("logsRetentionDays")) {
            retentionConfig.setLogsDays(((Number) policy.get("logsRetentionDays")).intValue());
        }
        if (policy.containsKey("testResultsRetentionDays")) {
            retentionConfig.setTestResultsDays(((Number) policy.get("testResultsRetentionDays")).intValue());
        }
        if (policy.containsKey("auditLogsRetentionDays")) {
            retentionConfig.setAuditLogsDays(((Number) policy.get("auditLogsRetentionDays")).intValue());
        }
        if (policy.containsKey("loginLogsRetentionDays")) {
            retentionConfig.setLoginLogsDays(((Number) policy.get("loginLogsRetentionDays")).intValue());
        }

        log.info("数据保留策略已更新: logs={}天, testResults={}天, auditLogs={}天, loginLogs={}天",
                retentionConfig.getLogsDays(), retentionConfig.getTestResultsDays(),
                retentionConfig.getAuditLogsDays(), retentionConfig.getLoginLogsDays());

        return getRetentionPolicy();
    }

    // ==================== Private Cleanup Methods ====================

    private long cleanupOldSystemLogs(LocalDateTime threshold) {
        try {
            long total = 0;
            for (var tenant : tenantRepository.findAll()) {
                total += systemLogRepository.deleteByTenantIdAndCreatedAtBefore(tenant.getId(), threshold);
            }
            return total;
        } catch (Exception e) {
            log.error("清理系统日志失败", e);
            return 0;
        }
    }

    private long cleanupOldTestResults(LocalDateTime threshold) {
        try {
            long total = 0;
            for (var tenant : tenantRepository.findAll()) {
                total += testResultRepository.deleteByTenantIdAndCreatedAtBefore(tenant.getId(), threshold);
            }
            return total;
        } catch (Exception e) {
            log.error("清理测试结果失败", e);
            return 0;
        }
    }

    private long cleanupOldLoginLogs(LocalDateTime threshold) {
        try {
            var allLogs = loginLogRepository.findByLoginTimeBetween(
                    LocalDateTime.MIN, threshold);
            long count = allLogs.size();
            loginLogRepository.deleteAll(allLogs);
            return count;
        } catch (Exception e) {
            log.error("清理登录日志失败", e);
            return 0;
        }
    }

    private long cleanupOldSessions(LocalDateTime threshold) {
        try {
            int deleted = userSessionRepository.deleteInactiveSessionsBefore(threshold);
            return deleted;
        } catch (Exception e) {
            log.error("清理过期会话失败", e);
            return 0;
        }
    }

    /**
     * 记录数据清理操作的审计日志
     */
    private void recordCleanupAuditLog(long totalDeleted, Long tenantId) {
        try {
            SystemLog auditLog = new SystemLog();
            auditLog.setTenantId(tenantId != null ? tenantId : TenantContextHolder.getTenantId());
            auditLog.setModule("数据保留策略");
            auditLog.setOperation("定时数据清理");
            auditLog.setIsSuccess(true);
            auditLog.setParams(String.format("总删除记录数=%d, logs=%d天, testResults=%d天, auditLogs=%d天, loginLogs=%d天",
                    totalDeleted,
                    retentionConfig.getLogsDays(),
                    retentionConfig.getTestResultsDays(),
                    retentionConfig.getAuditLogsDays(),
                    retentionConfig.getLoginLogsDays()));
            systemLogService.saveLog(auditLog);
        } catch (Exception e) {
            log.error("记录数据清理审计日志失败", e);
        }
    }
}
