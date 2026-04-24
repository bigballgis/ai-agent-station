package com.aiagent.repository;

import com.aiagent.entity.ApiCallAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiCallAuditLogRepository extends JpaRepository<ApiCallAuditLog, Long> {

    Page<ApiCallAuditLog> findByTenantId(Long tenantId, Pageable pageable);

    Page<ApiCallAuditLog> findByTenantIdAndResponseStatus(Long tenantId, Integer responseStatus, Pageable pageable);

    Page<ApiCallAuditLog> findByRequestId(String requestId, Pageable pageable);

    @Query("SELECT a FROM ApiCallAuditLog a WHERE a.tenantId = :tenantId AND a.createdAt >= :startTime ORDER BY a.createdAt DESC")
    Page<ApiCallAuditLog> findByTenantIdAndCreatedAtAfter(@Param("tenantId") Long tenantId,
                                                           @Param("startTime") LocalDateTime startTime,
                                                           Pageable pageable);

    @Query("SELECT COUNT(a) FROM ApiCallAuditLog a WHERE a.tenantId = :tenantId AND a.createdAt >= :startTime")
    Long countByTenantIdAndCreatedAtAfter(@Param("tenantId") Long tenantId,
                                          @Param("startTime") LocalDateTime startTime);

    @Query("SELECT a.responseStatus, COUNT(a) FROM ApiCallAuditLog a WHERE a.tenantId = :tenantId AND a.createdAt >= :startTime GROUP BY a.responseStatus")
    List<Object[]> countByResponseStatusByTenantIdAndCreatedAtAfter(@Param("tenantId") Long tenantId,
                                                                     @Param("startTime") LocalDateTime startTime);

    @Query("SELECT a.requestPath, COUNT(a) FROM ApiCallAuditLog a WHERE a.tenantId = :tenantId AND a.createdAt >= :startTime GROUP BY a.requestPath ORDER BY COUNT(a) DESC")
    List<Object[]> findTopPathsByCallCount(@Param("tenantId") Long tenantId,
                                           @Param("startTime") LocalDateTime startTime,
                                           Pageable pageable);
}
