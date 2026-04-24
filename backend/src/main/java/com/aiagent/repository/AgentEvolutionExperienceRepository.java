package com.aiagent.repository;

import com.aiagent.entity.AgentEvolutionExperience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface AgentEvolutionExperienceRepository extends JpaRepository<AgentEvolutionExperience, Long>, JpaSpecificationExecutor<AgentEvolutionExperience> {

    List<AgentEvolutionExperience> findByTenantId(Long tenantId);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentEvolutionExperience> findByTenantId(Long tenantId, Pageable pageable);

    Optional<AgentEvolutionExperience> findByIdAndTenantId(Long id, Long tenantId);

    List<AgentEvolutionExperience> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    List<AgentEvolutionExperience> findByExperienceTypeAndTenantId(String experienceType, Long tenantId);

    Optional<AgentEvolutionExperience> findByExperienceCodeAndTenantId(String experienceCode, Long tenantId);

    @Query("SELECT AVG(e.effectivenessScore) FROM AgentEvolutionExperience e WHERE e.tenantId = :tenantId AND e.effectivenessScore IS NOT NULL")
    Optional<BigDecimal> averageEffectivenessScoreByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT AVG(e.effectivenessScore) FROM AgentEvolutionExperience e WHERE e.effectivenessScore IS NOT NULL")
    @Deprecated
    Optional<BigDecimal> averageEffectivenessScoreGlobal();

    @Query("SELECT e.experienceType, COUNT(e), AVG(e.effectivenessScore) FROM AgentEvolutionExperience e WHERE e.tenantId = :tenantId GROUP BY e.experienceType")
    List<Object[]> countAndAvgScoreByTypeForTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT e.experienceType, COUNT(e), AVG(e.effectivenessScore) FROM AgentEvolutionExperience e GROUP BY e.experienceType")
    @Deprecated
    List<Object[]> countAndAvgScoreByTypeGlobal();

    @Query("SELECT COALESCE(SUM(e.usageCount), 0) FROM AgentEvolutionExperience e WHERE e.tenantId = :tenantId")
    long totalUsageCountByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(e.usageCount), 0) FROM AgentEvolutionExperience e")
    @Deprecated
    long totalUsageCountGlobal();
}
