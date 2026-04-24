package com.aiagent.repository;

import com.aiagent.entity.AgentTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgentTestResultRepository extends JpaRepository<AgentTestResult, Long> {

    /**
     * 根据执行ID查询测试结果
     */
    List<AgentTestResult> findByExecutionIdAndTenantId(Long executionId, Long tenantId);

    @Deprecated
    List<AgentTestResult> findByExecutionId(Long executionId);

    /**
     * 根据租户ID查询测试结果
     */
    List<AgentTestResult> findByTenantId(Long tenantId);

    /**
     * 根据Agent ID查询测试结果
     */
    List<AgentTestResult> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    List<AgentTestResult> findByAgentId(Long agentId);

    /**
     * 根据测试用例ID查询测试结果
     */
    List<AgentTestResult> findByTestCaseIdAndTenantId(Long testCaseId, Long tenantId);

    @Deprecated
    List<AgentTestResult> findByTestCaseId(Long testCaseId);

    /**
     * 根据租户ID和状态查询测试结果
     * @param tenantId 租户ID
     * @param status 状态
     * @return 测试结果列表
     */
    List<AgentTestResult> findByTenantIdAndStatus(Long tenantId, String status);

    /**
     * 统计租户的测试结果数量
     * @param tenantId 租户ID
     * @return 测试结果数量
     */
    long countByTenantId(Long tenantId);

    /**
     * 统计Agent的测试结果数量
     */
    long countByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    long countByAgentId(Long agentId);

    /**
     * 统计测试用例的测试结果数量
     */
    long countByTestCaseIdAndTenantId(Long testCaseId, Long tenantId);

    @Deprecated
    long countByTestCaseId(Long testCaseId);

    /**
     * 统计执行的测试结果数量
     */
    long countByExecutionIdAndTenantId(Long executionId, Long tenantId);

    @Deprecated
    long countByExecutionId(Long executionId);

    @Modifying
    @Query("DELETE FROM AgentTestResult r WHERE r.tenantId = :tenantId AND r.createdAt < :threshold")
    int deleteByTenantIdAndCreatedAtBefore(@Param("tenantId") Long tenantId, @Param("threshold") LocalDateTime threshold);

    @Deprecated
    @Modifying
    @Query("DELETE FROM AgentTestResult r WHERE r.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * 统计Agent下指定状态的测试结果数量（用于通过率计算）
     */
    @Query("SELECT COUNT(r) FROM AgentTestResult r WHERE r.agentId = :agentId AND r.status = :status")
    long countByAgentIdAndStatus(@Param("agentId") Long agentId, @Param("status") String status);

    /**
     * 统计测试用例下指定状态的测试结果数量（用于通过率计算）
     */
    @Query("SELECT COUNT(r) FROM AgentTestResult r WHERE r.testCaseId = :testCaseId AND r.status = :status")
    long countByTestCaseIdAndStatus(@Param("testCaseId") Long testCaseId, @Param("status") String status);
}