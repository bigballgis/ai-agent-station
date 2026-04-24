package com.aiagent.repository;

import com.aiagent.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {
    Page<AlertRecord> findByTenantIdOrderByFiredAtDesc(Long tenantId, Pageable pageable);

    List<AlertRecord> findByTenantIdAndStatus(Long tenantId, String status);

    Page<AlertRecord> findByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);

    long countByTenantIdAndStatusAndFiredAtAfter(Long tenantId, String status, LocalDateTime since);

    @Deprecated
    List<AlertRecord> findByStatus(String status);

    @Deprecated
    List<AlertRecord> findByStatus(String status, Pageable pageable);

    @Deprecated
    long countByStatusAndFiredAtAfter(String status, LocalDateTime since);
}
