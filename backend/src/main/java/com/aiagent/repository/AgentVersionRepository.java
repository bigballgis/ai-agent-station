package com.aiagent.repository;

import com.aiagent.entity.AgentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
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

    /**
     * 统计Agent的版本数量
     */
    long countByAgentIdAndTenantId(Long agentId, Long tenantId);

    /**
     * 分页查询Agent版本列表
     */
    Page<AgentVersion> findByAgentIdAndTenantId(Long agentId, Long tenantId, Pageable pageable);
}
