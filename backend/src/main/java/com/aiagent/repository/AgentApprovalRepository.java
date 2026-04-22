package com.aiagent.repository;

import com.aiagent.entity.AgentApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentApprovalRepository extends JpaRepository<AgentApproval, Long> {

    List<AgentApproval> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    Page<AgentApproval> findByTenantIdAndStatus(Long tenantId, AgentApproval.ApprovalStatus status, Pageable pageable);

    Page<AgentApproval> findByTenantId(Long tenantId, Pageable pageable);

    Optional<AgentApproval> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT a FROM AgentApproval a WHERE a.tenantId = :tenantId AND a.agentId = :agentId ORDER BY a.submittedAt DESC")
    List<AgentApproval> findLatestByAgentId(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);
}
