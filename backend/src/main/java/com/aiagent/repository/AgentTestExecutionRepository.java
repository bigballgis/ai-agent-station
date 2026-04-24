package com.aiagent.repository;

import com.aiagent.entity.AgentTestExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface AgentTestExecutionRepository extends JpaRepository<AgentTestExecution, Long> {

    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"testCase"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestExecution> findByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByAgentId(Long agentId);

    @EntityGraph(attributePaths = {"testCase"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestExecution> findByAgentIdAndTenantId(Long agentId, Long tenantId, Pageable pageable);

    @Deprecated
    @EntityGraph(attributePaths = {"testCase"})
    Page<AgentTestExecution> findByAgentId(Long agentId, Pageable pageable);

    List<AgentTestExecution> findByTestCaseIdAndTenantId(Long testCaseId, Long tenantId);

    @Deprecated
    List<AgentTestExecution> findByTestCaseId(Long testCaseId);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestExecution> findByTestCaseIdAndTenantId(Long testCaseId, Long tenantId, Pageable pageable);

    @Deprecated
    Page<AgentTestExecution> findByTestCaseId(Long testCaseId, Pageable pageable);

    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByTenantIdAndStatus(Long tenantId, Integer status);

    @EntityGraph(attributePaths = {"testCase"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestExecution> findByTenantIdAndStatus(Long tenantId, Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByTenantIdAndExecutionType(Long tenantId, String executionType);

    @EntityGraph(attributePaths = {"testCase"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestExecution> findByTenantIdAndExecutionType(Long tenantId, String executionType, Pageable pageable);

    long countByTenantId(Long tenantId);

    long countByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    long countByAgentId(Long agentId);

    long countByTestCaseIdAndTenantId(Long testCaseId, Long tenantId);

    @Deprecated
    long countByTestCaseId(Long testCaseId);
}
