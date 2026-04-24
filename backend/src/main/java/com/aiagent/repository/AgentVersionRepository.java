package com.aiagent.repository;

import com.aiagent.entity.AgentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentVersionRepository extends JpaRepository<AgentVersion, Long> {
    
    List<AgentVersion> findByAgentIdAndTenantIdOrderByVersionNumberDesc(Long agentId, Long tenantId);

    @Deprecated
    List<AgentVersion> findByAgentIdOrderByVersionNumberDesc(Long agentId);
    
    List<AgentVersion> findByTenantId(Long tenantId);
    
    Optional<AgentVersion> findByAgentIdAndVersionNumberAndTenantId(Long agentId, Integer versionNumber, Long tenantId);

    @Deprecated
    Optional<AgentVersion> findByAgentIdAndVersionNumber(Long agentId, Integer versionNumber);
    
    Optional<AgentVersion> findFirstByAgentIdAndTenantIdOrderByVersionNumberDesc(Long agentId, Long tenantId);

    @Deprecated
    Optional<AgentVersion> findFirstByAgentIdOrderByVersionNumberDesc(Long agentId);

    List<AgentVersion> findByAgentIdAndTenantIdOrderByVersionNumberDesc(Long agentId, Long tenantId);
}
