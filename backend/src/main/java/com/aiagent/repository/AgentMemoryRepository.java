package com.aiagent.repository;

import com.aiagent.entity.AgentMemory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {

    List<AgentMemory> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    List<AgentMemory> findByAgentIdAndSessionIdAndTenantId(Long agentId, String sessionId, Long tenantId);

    @Deprecated
    List<AgentMemory> findByAgentIdAndSessionId(Long agentId, String sessionId);

    List<AgentMemory> findByAgentIdAndMemoryTypeAndTenantId(Long agentId, AgentMemory.MemoryType memoryType, Long tenantId);

    @Deprecated
    List<AgentMemory> findByAgentIdAndMemoryType(Long agentId, AgentMemory.MemoryType memoryType);

    Page<AgentMemory> findByAgentIdAndTenantId(Long agentId, Long tenantId, Pageable pageable);

    @Query("SELECT m FROM AgentMemory m WHERE m.agentId = :agentId AND m.tenantId = :tenantId " +
           "AND (:keyword IS NULL OR m.content LIKE %:keyword% OR m.summary LIKE %:keyword%) " +
           "AND (:memoryType IS NULL OR m.memoryType = :memoryType)")
    Page<AgentMemory> searchMemories(@Param("agentId") Long agentId,
                                      @Param("tenantId") Long tenantId,
                                      @Param("keyword") String keyword,
                                      @Param("memoryType") AgentMemory.MemoryType memoryType,
                                      Pageable pageable);

    @Query("SELECT m FROM AgentMemory m WHERE m.tenantId = :tenantId AND m.expiresAt IS NOT NULL AND m.expiresAt < :now")
    List<AgentMemory> findExpiredMemories(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    long countByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Modifying
    void deleteByExpiresAtBeforeAndTenantId(LocalDateTime now, Long tenantId);

    @Deprecated
    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime now);
}
