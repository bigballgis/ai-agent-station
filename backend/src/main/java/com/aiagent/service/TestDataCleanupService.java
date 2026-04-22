package com.aiagent.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

@Service
public class TestDataCleanupService {

    private static final Logger logger = Logger.getLogger(TestDataCleanupService.class.getName());

    @Autowired
    private EntityManager entityManager;

    /**
     * 定时清理过期的测试数据
     * 每天凌晨执行
     */
    @Scheduled(cron = "${ai-agent.test.cleanup.cron-expression:0 0 0 * * ?}")
    @Transactional
    public void cleanupExpiredTestData() {
        logger.info("开始清理过期的测试数据");

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
            logger.info("清理了 " + resultDeleted + " 条过期的测试结果记录");

            // 清理过期的测试执行记录
            Query executionQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_executions WHERE created_at < :cutoffDate"
            );
            executionQuery.setParameter("cutoffDate", cutoffDate);
            int executionDeleted = executionQuery.executeUpdate();
            logger.info("清理了 " + executionDeleted + " 条过期的测试执行记录");

            // 注意：测试用例通常不自动清理，因为它们是配置数据

            logger.info("测试数据清理完成");
        } catch (Exception e) {
            logger.severe("清理测试数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 手动触发测试数据清理
     */
    @Transactional
    public void manualCleanup() {
        cleanupExpiredTestData();
    }

    /**
     * 清理指定租户的测试数据
     * @param tenantId 租户ID
     */
    @Transactional
    public void cleanupTenantTestData(Long tenantId) {
        logger.info("开始清理租户 " + tenantId + " 的测试数据");

        try {
            // 清理指定租户的测试结果
            Query resultQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_results WHERE tenant_id = :tenantId"
            );
            resultQuery.setParameter("tenantId", tenantId);
            int resultDeleted = resultQuery.executeUpdate();
            logger.info("清理了租户 " + tenantId + " 的 " + resultDeleted + " 条测试结果记录");

            // 清理指定租户的测试执行记录
            Query executionQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_executions WHERE tenant_id = :tenantId"
            );
            executionQuery.setParameter("tenantId", tenantId);
            int executionDeleted = executionQuery.executeUpdate();
            logger.info("清理了租户 " + tenantId + " 的 " + executionDeleted + " 条测试执行记录");

            // 注意：测试用例通常不自动清理，因为它们是配置数据

            logger.info("租户 " + tenantId + " 的测试数据清理完成");
        } catch (Exception e) {
            logger.severe("清理租户 " + tenantId + " 的测试数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清理指定租户和Agent的测试数据
     * @param tenantId 租户ID
     * @param agentId Agent ID
     */
    @Transactional
    public void cleanupTestData(Long tenantId, Long agentId) {
        logger.info("开始清理租户 " + tenantId + " Agent " + agentId + " 的测试数据");

        try {
            // 清理指定租户和Agent的测试结果
            Query resultQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_results WHERE tenant_id = :tenantId AND agent_id = :agentId"
            );
            resultQuery.setParameter("tenantId", tenantId);
            resultQuery.setParameter("agentId", agentId);
            int resultDeleted = resultQuery.executeUpdate();
            logger.info("清理了租户 " + tenantId + " Agent " + agentId + " 的 " + resultDeleted + " 条测试结果记录");

            // 清理指定租户和Agent的测试执行记录
            Query executionQuery = entityManager.createNativeQuery(
                "DELETE FROM agent_test_executions WHERE tenant_id = :tenantId AND agent_id = :agentId"
            );
            executionQuery.setParameter("tenantId", tenantId);
            executionQuery.setParameter("agentId", agentId);
            int executionDeleted = executionQuery.executeUpdate();
            logger.info("清理了租户 " + tenantId + " Agent " + agentId + " 的 " + executionDeleted + " 条测试执行记录");

            // 注意：测试用例通常不自动清理，因为它们是配置数据

            logger.info("租户 " + tenantId + " Agent " + agentId + " 的测试数据清理完成");
        } catch (Exception e) {
            logger.severe("清理租户 " + tenantId + " Agent " + agentId + " 的测试数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}