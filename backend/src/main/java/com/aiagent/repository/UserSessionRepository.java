package com.aiagent.repository;

import com.aiagent.entity.UserSession;
import com.aiagent.entity.UserSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * Find a session by its session ID (JWT jti claim).
     */
    Optional<UserSession> findBySessionId(String sessionId);

    /**
     * Find all sessions for a specific user.
     */
    List<UserSession> findByUserId(Long userId);

    /**
     * Find all active sessions for a specific user.
     */
    List<UserSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    /**
     * Find all active sessions across all users.
     */
    List<UserSession> findByStatus(SessionStatus status);

    /**
     * Find all sessions that have expired but are still marked as ACTIVE.
     */
    @Query("SELECT s FROM UserSession s WHERE s.status = 'ACTIVE' AND s.expireTime < :now")
    List<UserSession> findExpiredActiveSessions(@Param("now") LocalDateTime now);

    /**
     * Invalidate all active sessions for a specific user (set status to KICKED).
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.status = 'KICKED' WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    int invalidateAllUserSessions(@Param("userId") Long userId);

    /**
     * Count active sessions for a specific user.
     */
    long countByUserIdAndStatus(Long userId, SessionStatus status);

    /**
     * Count all active sessions.
     */
    long countByStatus(SessionStatus status);

    /**
     * Delete sessions that have been inactive beyond the specified time.
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.lastAccessTime < :threshold AND s.status != 'ACTIVE'")
    int deleteInactiveSessionsBefore(@Param("threshold") LocalDateTime threshold);
}
