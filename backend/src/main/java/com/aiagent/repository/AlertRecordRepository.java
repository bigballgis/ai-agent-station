package com.aiagent.repository;

import com.aiagent.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AlertRecord> findByTenantIdOrderByFiredAtDesc(Long tenantId, Pageable pageable);

    List<AlertRecord> findByTenantIdAndStatus(Long tenantId, String status);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<AlertRecord> findByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);

    long countByTenantIdAndStatusAndFiredAtAfter(Long tenantId, String status, LocalDateTime since);

    @Deprecated
    List<AlertRecord> findByStatus(String status);

    @Deprecated
    Page<AlertRecord> findByStatus(String status, Pageable pageable);

    @Deprecated
    long countByStatusAndFiredAtAfter(String status, LocalDateTime since);
}
