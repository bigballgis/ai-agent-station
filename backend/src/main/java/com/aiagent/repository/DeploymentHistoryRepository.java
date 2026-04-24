package com.aiagent.repository;

import com.aiagent.entity.DeploymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface DeploymentHistoryRepository extends JpaRepository<DeploymentHistory, Long> {

    @EntityGraph(attributePaths = {"agent"})
    List<DeploymentHistory> findByAgentIdAndTenantIdOrderByCreatedAtDesc(Long agentId, Long tenantId);

    @EntityGraph(attributePaths = {"agent"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<DeploymentHistory> findByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"agent"})
    Optional<DeploymentHistory> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"agent"})
    @Query("SELECT d FROM DeploymentHistory d WHERE d.tenantId = :tenantId AND d.agentId = :agentId AND d.status = 'SUCCESS' ORDER BY d.deployedAt DESC")
    List<DeploymentHistory> findSuccessfulDeployments(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);

    @EntityGraph(attributePaths = {"agent"})
    @Query("SELECT d FROM DeploymentHistory d WHERE d.tenantId = :tenantId AND d.agentId = :agentId ORDER BY d.createdAt DESC LIMIT 1")
    Optional<DeploymentHistory> findLatestDeployment(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);
}
