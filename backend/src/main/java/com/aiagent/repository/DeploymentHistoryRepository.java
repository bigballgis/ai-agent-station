package com.aiagent.repository;

import com.aiagent.entity.DeploymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentHistoryRepository extends JpaRepository<DeploymentHistory, Long> {

    List<DeploymentHistory> findByAgentIdAndTenantIdOrderByCreatedAtDesc(Long agentId, Long tenantId);

    Page<DeploymentHistory> findByTenantId(Long tenantId, Pageable pageable);

    Optional<DeploymentHistory> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT d FROM DeploymentHistory d WHERE d.tenantId = :tenantId AND d.agentId = :agentId AND d.status = 'SUCCESS' ORDER BY d.deployedAt DESC")
    List<DeploymentHistory> findSuccessfulDeployments(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);

    @Query("SELECT d FROM DeploymentHistory d WHERE d.tenantId = :tenantId AND d.agentId = :agentId ORDER BY d.createdAt DESC LIMIT 1")
    Optional<DeploymentHistory> findLatestDeployment(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);
}
