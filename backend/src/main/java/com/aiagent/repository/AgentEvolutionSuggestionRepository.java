package com.aiagent.repository;

import com.aiagent.entity.AgentEvolutionSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentEvolutionSuggestionRepository extends JpaRepository<AgentEvolutionSuggestion, Long>, JpaSpecificationExecutor<AgentEvolutionSuggestion> {
    
    List<AgentEvolutionSuggestion> findByTenantId(Long tenantId);

    Page<AgentEvolutionSuggestion> findByTenantId(Long tenantId, Pageable pageable);
    
    Optional<AgentEvolutionSuggestion> findByIdAndTenantId(Long id, Long tenantId);
    
    List<AgentEvolutionSuggestion> findByAgentIdAndTenantId(Long agentId, Long tenantId);
    
    List<AgentEvolutionSuggestion> findByReflectionIdAndTenantId(Long reflectionId, Long tenantId);
    
    List<AgentEvolutionSuggestion> findByReflectionId(Long reflectionId);
    
    List<AgentEvolutionSuggestion> findByStatusAndTenantId(String status, Long tenantId);
    
    List<AgentEvolutionSuggestion> findByImplementationStatusAndTenantId(String implementationStatus, Long tenantId);
    
    List<AgentEvolutionSuggestion> findBySuggestionTypeAndTenantId(String suggestionType, Long tenantId);
}
