package com.aiagent.repository;

import com.aiagent.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long>, JpaSpecificationExecutor<LoginLog> {
    List<LoginLog> findByUserIdOrderByLoginTimeDesc(Long userId);

    List<LoginLog> findByLoginTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByUsernameAndLoginTimeAfterAndStatus(String username, LocalDateTime time, String status);
}
