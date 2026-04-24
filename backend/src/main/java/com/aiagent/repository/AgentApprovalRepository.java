package com.aiagent.repository;

import com.aiagent.entity.AgentApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentApprovalRepository extends JpaRepository<AgentApproval, Long> {

    @EntityGraph(attributePaths = {"agent", "agentVersion"})
    List<AgentApproval> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    @EntityGraph(attributePaths = {"agent"})
    Page<AgentApproval> findByTenantIdAndStatus(Long tenantId, AgentApproval.ApprovalStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"agent"})
    Page<AgentApproval> findByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"agent", "agentVersion"})
    Optional<AgentApproval> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"agent", "agentVersion"})
    @Query("SELECT a FROM AgentApproval a WHERE a.tenantId = :tenantId AND a.agentId = :agentId ORDER BY a.submittedAt DESC")
    List<AgentApproval> findLatestByAgentId(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);
}
