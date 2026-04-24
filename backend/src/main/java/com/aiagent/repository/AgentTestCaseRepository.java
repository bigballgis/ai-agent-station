package com.aiagent.repository;

import com.aiagent.entity.AgentTestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface AgentTestCaseRepository extends JpaRepository<AgentTestCase, Long> {

    List<AgentTestCase> findByTenantId(Long tenantId);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestCase> findByTenantId(Long tenantId, Pageable pageable);

    List<AgentTestCase> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    List<AgentTestCase> findByAgentId(Long agentId);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestCase> findByAgentIdAndTenantId(Long agentId, Long tenantId, Pageable pageable);

    @Deprecated
    Page<AgentTestCase> findByAgentId(Long agentId, Pageable pageable);

    AgentTestCase findByTenantIdAndTestCode(Long tenantId, String testCode);

    List<AgentTestCase> findByTenantIdAndStatus(Long tenantId, Integer status);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestCase> findByTenantIdAndStatus(Long tenantId, Integer status, Pageable pageable);

    List<AgentTestCase> findByTenantIdAndTestType(Long tenantId, String testType);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentTestCase> findByTenantIdAndTestType(Long tenantId, String testType, Pageable pageable);

    long countByTenantId(Long tenantId);

    long countByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    long countByAgentId(Long agentId);

    /**
     * 检查租户下是否存在指定测试代码
     */
    boolean existsByTenantIdAndTestCode(Long tenantId, String testCode);
}
