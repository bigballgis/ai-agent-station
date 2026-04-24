package com.aiagent.repository;

import com.aiagent.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findByTenantId(Long tenantId);

    Page<SystemLog> findByTenantId(Long tenantId, Pageable pageable);

    Page<SystemLog> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    Page<SystemLog> findByTenantIdAndModule(Long tenantId, String module, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SystemLog s WHERE s.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
