package com.aiagent.repository;

import com.aiagent.entity.ApiCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {

    Optional<ApiCallLog> findByRequestId(String requestId);

    Page<ApiCallLog> findByTenantId(Long tenantId, Pageable pageable);

    Page<ApiCallLog> findByTenantIdAndAgentId(Long tenantId, Long agentId, Pageable pageable);

    Page<ApiCallLog> findByTenantIdAndApiInterfaceId(Long tenantId, Long apiInterfaceId, Pageable pageable);

    List<ApiCallLog> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT COUNT(a) FROM ApiCallLog a WHERE a.tenantId = :tenantId AND a.agentId = :agentId AND a.createdAt >= :startTime")
    Long countByTenantIdAndAgentIdAndCreatedAtAfter(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT AVG(a.executionTime) FROM ApiCallLog a WHERE a.tenantId = :tenantId AND a.agentId = :agentId AND a.createdAt >= :startTime")
    Double findAverageExecutionTimeByTenantIdAndAgentIdAndCreatedAtAfter(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId, @Param("startTime") LocalDateTime startTime);
}
