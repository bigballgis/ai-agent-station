package com.aiagent.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class TestDataCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TestDataCleanupService.class);

    @Autowired
    private EntityManager entityManager;

    /**
     * 定时清理过期的测试数据
     * 每天凌晨执行
     */
    @Scheduled(cron = "${ai-agent.test.cleanup.cron-expression:0 0 0 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredTestData() {
        log.info("开始清理过期的测试数据");

        try {
            // 获取保留天数配置
            int retentionDays = 30; // 默认保留30天

            // 计算过期时间
            LocalDateTime cutoffDate = LocalDateTime.now().minus(retentionDays, ChronoUnit.DAYS);

            // 清理过期的测试结果
            Query resultQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_results WHERE created_at < :cutoffDate"
            );
            resultQuery.setParameter("cutoffDate", cutoffDate);
            int resultDeleted = resultQuery.executeUpdate();
            log.info("清理了 {} 条过期的测试结果记录", resultDeleted);

            // 清理过期的测试执行记录
            Query executionQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_executions WHERE created_at < :cutoffDate"
            );
            executionQuery.setParameter("cutoffDate", cutoffDate);
            int executionDeleted = executionQuery.executeUpdate();
            log.info("清理了 {} 条过期的测试执行记录", executionDeleted);

            // 注意：测试用例通常不自动清理，因为它们是配置数据

            log.info("测试数据清理完成");
        } catch (Exception e) {
            log.error("清理过期测试数据失败", e);
        }
    }

    /**
     * 手动触发测试数据清理
     */
    @Transactional(rollbackFor = Exception.class)
    public void manualCleanup() {
        cleanupExpiredTestData();
    }

    /**
     * 清理指定租户的测试数据
     * @param tenantId 租户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupTenantTestData(Long tenantId) {
        log.info("开始清理租户 {} 的测试数据", tenantId);

        try {
            // 清理指定租户的测试结果
            Query resultQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_results WHERE tenant_id = :tenantId"
            );
            resultQuery.setParameter("tenantId", tenantId);
            int resultDeleted = resultQuery.executeUpdate();
            log.info("清理了租户 {} 的 {} 条测试结果记录", tenantId, resultDeleted);

            // 清理指定租户的测试执行记录
            Query executionQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_executions WHERE tenant_id = :tenantId"
            );
            executionQuery.setParameter("tenantId", tenantId);
            int executionDeleted = executionQuery.executeUpdate();
            log.info("清理了租户 {} 的 {} 条测试执行记录", tenantId, executionDeleted);

            // 注意：测试用例通常不自动清理，因为它们是配置数据

            log.info("租户 {} 的测试数据清理完成", tenantId);
        } catch (Exception e) {
            log.error("清理租户 {} 的测试数据失败", tenantId, e);
        }
    }

    /**
     * 清理指定租户和Agent的测试数据
     * @param tenantId 租户ID
     * @param agentId Agent ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupTestData(Long tenantId, Long agentId) {
        log.info("开始清理租户 {} Agent {} 的测试数据", tenantId, agentId);

        try {
            // 清理指定租户和Agent的测试结果
            Query resultQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_results WHERE tenant_id = :tenantId AND agent_id = :agentId"
            );
            resultQuery.setParameter("tenantId", tenantId);
            resultQuery.setParameter("agentId", agentId);
            int resultDeleted = resultQuery.executeUpdate();
            log.info("清理了租户 {} Agent {} 的 {} 条测试结果记录", tenantId, agentId, resultDeleted);

            // 清理指定租户和Agent的测试执行记录
            Query executionQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_executions WHERE tenant_id = :tenantId AND agent_id = :agentId"
            );
            executionQuery.setParameter("tenantId", tenantId);
            executionQuery.setParameter("agentId", agentId);
            int executionDeleted = executionQuery.executeUpdate();
            log.info("清理了租户 {} Agent {} 的 {} 条测试执行记录", tenantId, agentId, executionDeleted);

            // 注意：测试用例通常不自动清理，因为它们是配置数据

            log.info("租户 {} Agent {} 的测试数据清理完成", tenantId, agentId);
        } catch (Exception e) {
            log.error("清理租户 {} Agent {} 的测试数据失败", tenantId, agentId, e);
        }
    }
}