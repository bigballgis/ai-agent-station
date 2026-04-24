package com.aiagent.repository;

import com.aiagent.entity.ExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, Long> {

    Page<ExecutionHistory> findByAgentIdAndTenantIdOrderByTimestampDesc(Long agentId, Long tenantId, Pageable pageable);

    @Deprecated
    Page<ExecutionHistory> findByAgentIdOrderByTimestampDesc(Long agentId, Pageable pageable);

    void deleteByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    void deleteByAgentId(Long agentId);
}
