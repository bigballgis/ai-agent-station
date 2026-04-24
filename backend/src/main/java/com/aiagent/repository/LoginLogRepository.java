package com.aiagent.repository;

import com.aiagent.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface LoginLogRepository extends JpaRepository<LoginLog, Long>, JpaSpecificationExecutor<LoginLog> {
    List<LoginLog> findByUserIdOrderByLoginTimeDesc(Long userId);

    List<LoginLog> findByLoginTimeBetween(LocalDateTime start, LocalDateTime end);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<LoginLog> findByLoginTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countByUsernameAndLoginTimeAfterAndStatus(String username, LocalDateTime time, String status);
}
