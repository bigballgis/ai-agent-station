package com.aiagent.repository;

import com.aiagent.entity.ApiInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface ApiInterfaceRepository extends JpaRepository<ApiInterface, Long> {

    @EntityGraph(attributePaths = {"agent"})
    List<ApiInterface> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    @EntityGraph(attributePaths = {"agent"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<ApiInterface> findByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"agent"})
    Optional<ApiInterface> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"agent"})
    List<ApiInterface> findByAgentIdAndTenantIdAndIsActive(Long agentId, Long tenantId, Boolean isActive);

    @EntityGraph(attributePaths = {"agent"})
    List<ApiInterface> findByBaseApiIdAndTenantIdOrderByApiVersionDesc(Long baseApiId, Long tenantId);

    @EntityGraph(attributePaths = {"agent"})
    List<ApiInterface> findByAgentIdAndTenantIdAndDeprecatedFalse(Long agentId, Long tenantId);
}
