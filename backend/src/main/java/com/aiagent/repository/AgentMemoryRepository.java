package com.aiagent.repository;

import com.aiagent.entity.AgentMemory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {

    List<AgentMemory> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    List<AgentMemory> findByAgentIdAndSessionIdAndTenantId(Long agentId, String sessionId, Long tenantId);

    List<AgentMemory> findByAgentIdAndMemoryTypeAndTenantId(Long agentId, AgentMemory.MemoryType memoryType, Long tenantId);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AgentMemory> findByAgentIdAndTenantId(Long agentId, Long tenantId, Pageable pageable);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query("SELECT m FROM AgentMemory m WHERE m.agentId = :agentId AND m.tenantId = :tenantId " +
           "AND (:keyword IS NULL OR :keyword = '' OR m.content LIKE CONCAT('%', :keyword, '%') OR m.summary LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:memoryType IS NULL OR m.memoryType = :memoryType)")
    Page<AgentMemory> searchMemories(@Param("agentId") Long agentId,
                                      @Param("tenantId") Long tenantId,
                                      @Param("keyword") String keyword,
                                      @Param("memoryType") AgentMemory.MemoryType memoryType,
                                      Pageable pageable);

    @Query("SELECT m FROM AgentMemory m WHERE m.tenantId = :tenantId AND m.expiresAt IS NOT NULL AND m.expiresAt < :now")
    List<AgentMemory> findExpiredMemories(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    long countByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Transactional
    @Modifying
    void deleteByExpiresAtBeforeAndTenantId(LocalDateTime now, Long tenantId);
}
