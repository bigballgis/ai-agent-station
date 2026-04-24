package com.aiagent.repository;

import com.aiagent.entity.ExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;

@Repository
@Transactional(readOnly = true)
public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, Long> {

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<ExecutionHistory> findByAgentIdAndTenantIdOrderByTimestampDesc(Long agentId, Long tenantId, Pageable pageable);

    @Deprecated
    Page<ExecutionHistory> findByAgentIdOrderByTimestampDesc(Long agentId, Pageable pageable);

    @Transactional
    void deleteByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    @Transactional
    void deleteByAgentId(Long agentId);
}
