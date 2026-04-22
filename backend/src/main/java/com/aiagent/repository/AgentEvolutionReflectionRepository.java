package com.aiagent.repository;

import com.aiagent.entity.AgentEvolutionReflection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentEvolutionReflectionRepository extends JpaRepository<AgentEvolutionReflection, Long> {
    
    List<AgentEvolutionReflection> findByTenantId(Long tenantId);
    
    Optional<AgentEvolutionReflection> findByIdAndTenantId(Long id, Long tenantId);
    
    List<AgentEvolutionReflection> findByAgentIdAndTenantId(Long agentId, Long tenantId);
    
    List<AgentEvolutionReflection> findByEvaluationTypeAndTenantId(String evaluationType, Long tenantId);
}
