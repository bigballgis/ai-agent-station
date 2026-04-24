package com.aiagent.repository;

import com.aiagent.entity.UserSession;
import com.aiagent.entity.UserSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionId(String sessionId);

    List<UserSession> findByUserId(Long userId);

    List<UserSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    List<UserSession> findByStatus(SessionStatus status);

    @Query("SELECT s FROM UserSession s WHERE s.status = 'ACTIVE' AND s.expireTime < :now")
    List<UserSession> findExpiredActiveSessions(@Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("UPDATE UserSession s SET s.status = 'KICKED' WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    int invalidateAllUserSessions(@Param("userId") Long userId);

    long countByUserIdAndStatus(Long userId, SessionStatus status);

    long countByStatus(SessionStatus status);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.lastAccessTime < :threshold AND s.status != 'ACTIVE'")
    int deleteInactiveSessionsBefore(@Param("threshold") LocalDateTime threshold);
}
