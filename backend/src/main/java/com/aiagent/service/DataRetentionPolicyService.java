package com.aiagent.service;

import com.aiagent.repository.SystemLogRepository;
import com.aiagent.repository.AgentTestResultRepository;
import com.aiagent.repository.LoginLogRepository;
import com.aiagent.repository.UserSessionRepository;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionPolicyService {

    private final SystemLogRepository systemLogRepository;
    private final AgentTestResultRepository testResultRepository;
    private final LoginLogRepository loginLogRepository;
    private final UserSessionRepository userSessionRepository;

    /** 默认保留天数 */
    private static final int DEFAULT_LOGS_DAYS = 90;
    private static final int DEFAULT_TEST_RESULTS_DAYS = 180;
    private static final int DEFAULT_AUDIT_LOGS_DAYS = 365;
    private static final int DEFAULT_LOGIN_LOGS_DAYS = 180;

    /** 可配置的保留天数 */
    private int logsRetentionDays = DEFAULT_LOGS_DAYS;
    private int testResultsRetentionDays = DEFAULT_TEST_RESULTS_DAYS;
    private int auditLogsRetentionDays = DEFAULT_AUDIT_LOGS_DAYS;
    private int loginLogsRetentionDays = DEFAULT_LOGIN_LOGS_DAYS;

    /**
     * 获取当前保留策略
     *
     * @return 保留策略配置
     */
    public Map<String, Object> getRetentionPolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("logs", Map.of("retentionDays", logsRetentionDays, "description", "系统日志保留天数"));
        policy.put("testResults", Map.of("retentionDays", testResultsRetentionDays, "description", "测试结果保留天数"));
        policy.put("auditLogs", Map.of("retentionDays", auditLogsRetentionDays, "description", "审计日志保留天数"));
        policy.put("loginLogs", Map.of("retentionDays", loginLogsRetentionDays, "description", "登录日志保留天数"));
        return policy;
    }

    /**
     * 执行保留策略清理（定时任务，每天凌晨3点执行）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void executeRetentionCleanup() {
        log.info("开始执行数据保留策略清理...");

        LocalDateTime now = LocalDateTime.now();

        // 清理过期系统日志
        LocalDateTime logsThreshold = now.minusDays(logsRetentionDays);
        long logsCount = cleanupOldSystemLogs(logsThreshold);
        log.info("清理系统日志完成: 删除{}条超过{}天的记录", logsCount, logsRetentionDays);

        // 清理过期测试结果
        LocalDateTime testResultsThreshold = now.minusDays(testResultsRetentionDays);
        long testResultsCount = cleanupOldTestResults(testResultsThreshold);
        log.info("清理测试结果完成: 删除{}条超过{}天的记录", testResultsCount, testResultsRetentionDays);

        // 清理过期登录日志
        LocalDateTime loginLogsThreshold = now.minusDays(loginLogsRetentionDays);
        long loginLogsCount = cleanupOldLoginLogs(loginLogsThreshold);
        log.info("清理登录日志完成: 删除{}条超过{}天的记录", loginLogsCount, loginLogsRetentionDays);

        // 清理过期非活跃会话
        long sessionsCount = cleanupOldSessions(now.minusDays(30));
        log.info("清理过期会话完成: 删除{}条超过30天的非活跃会话", sessionsCount);

        log.info("数据保留策略清理全部完成");
    }

    /**
     * 更新保留策略
     *
     * @param policy 新的保留策略
     * @return 更新后的保留策略
     */
    public Map<String, Object> updateRetentionPolicy(Map<String, Object> policy) {
        if (policy.containsKey("logsRetentionDays")) {
            this.logsRetentionDays = ((Number) policy.get("logsRetentionDays")).intValue();
        }
        if (policy.containsKey("testResultsRetentionDays")) {
            this.testResultsRetentionDays = ((Number) policy.get("testResultsRetentionDays")).intValue();
        }
        if (policy.containsKey("auditLogsRetentionDays")) {
            this.auditLogsRetentionDays = ((Number) policy.get("auditLogsRetentionDays")).intValue();
        }
        if (policy.containsKey("loginLogsRetentionDays")) {
            this.loginLogsRetentionDays = ((Number) policy.get("loginLogsRetentionDays")).intValue();
        }

        log.info("数据保留策略已更新: logs={}天, testResults={}天, auditLogs={}天, loginLogs={}天",
                logsRetentionDays, testResultsRetentionDays, auditLogsRetentionDays, loginLogsRetentionDays);

        return getRetentionPolicy();
    }

    // ==================== Private Cleanup Methods ====================

    private long cleanupOldSystemLogs(LocalDateTime threshold) {
        try {
            return systemLogRepository.deleteByCreatedAtBefore(threshold);
        } catch (Exception e) {
            log.error("清理系统日志失败", e);
            return 0;
        }
    }

    private long cleanupOldTestResults(LocalDateTime threshold) {
        try {
            return testResultRepository.deleteByCreatedAtBefore(threshold);
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
}
