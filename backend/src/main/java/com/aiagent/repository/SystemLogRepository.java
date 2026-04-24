package com.aiagent.repository;

import com.aiagent.entity.SystemLog;
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
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findByTenantId(Long tenantId);

    Page<SystemLog> findByTenantId(Long tenantId, Pageable pageable);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<SystemLog> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    Page<SystemLog> findByTenantIdAndModule(Long tenantId, String module, Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM SystemLog s WHERE s.tenantId = :tenantId AND s.createdAt < :threshold")
    int deleteByTenantIdAndCreatedAtBefore(@Param("tenantId") Long tenantId, @Param("threshold") LocalDateTime threshold);
}
