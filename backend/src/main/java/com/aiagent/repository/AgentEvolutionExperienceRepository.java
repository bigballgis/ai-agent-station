package com.aiagent.repository;

import com.aiagent.entity.AgentEvolutionExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentEvolutionExperienceRepository extends JpaRepository<AgentEvolutionExperience, Long>, JpaSpecificationExecutor<AgentEvolutionExperience> {
    
    List<AgentEvolutionExperience> findByTenantId(Long tenantId);
    
    Optional<AgentEvolutionExperience> findByIdAndTenantId(Long id, Long tenantId);
    
    List<AgentEvolutionExperience> findByAgentIdAndTenantId(Long agentId, Long tenantId);
    
    List<AgentEvolutionExperience> findByExperienceTypeAndTenantId(String experienceType, Long tenantId);
    
    Optional<AgentEvolutionExperience> findByExperienceCodeAndTenantId(String experienceCode, Long tenantId);
}
