package com.aiagent.repository;

import com.aiagent.entity.AgentTestResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface AgentTestResultRepository extends JpaRepository<AgentTestResult, Long> {

    @EntityGraph(attributePaths = {"execution", "testCase"})
    List<AgentTestResult> findByExecutionIdAndTenantId(Long executionId, Long tenantId);

    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestResult> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestResult> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    @EntityGraph(attributePaths = {"execution"})
    List<AgentTestResult> findByTestCaseIdAndTenantId(Long testCaseId, Long tenantId);

    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestResult> findByTenantIdAndStatus(Long tenantId, String status);

    long countByTenantId(Long tenantId);

    long countByAgentIdAndTenantId(Long agentId, Long tenantId);

    long countByTestCaseIdAndTenantId(Long testCaseId, Long tenantId);

    long countByExecutionIdAndTenantId(Long executionId, Long tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentTestResult r WHERE r.tenantId = :tenantId AND r.createdAt < :threshold")
    int deleteByTenantIdAndCreatedAtBefore(@Param("tenantId") Long tenantId, @Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(r) FROM AgentTestResult r WHERE r.agentId = :agentId AND r.status = :status")
    long countByAgentIdAndStatus(@Param("agentId") Long agentId, @Param("status") String status);

    @Query("SELECT COUNT(r) FROM AgentTestResult r WHERE r.testCaseId = :testCaseId AND r.status = :status")
    long countByTestCaseIdAndStatus(@Param("testCaseId") Long testCaseId, @Param("status") String status);
}
