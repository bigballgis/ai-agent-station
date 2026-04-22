package com.aiagent.repository;

import com.aiagent.entity.Agent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    @EntityGraph(attributePaths = {"tenant"})
    List<Agent> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<Agent> findByIdAndTenantId(Long id, Long tenantId);

    List<Agent> findByTenantIdAndIsActive(Long tenantId, Boolean isActive);

    boolean existsByNameAndTenantId(String name, Long tenantId);
}
