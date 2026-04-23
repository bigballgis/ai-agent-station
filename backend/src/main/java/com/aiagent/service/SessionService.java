package com.aiagent.service;

import com.aiagent.entity.UserSession;
import com.aiagent.entity.UserSession.SessionStatus;
import com.aiagent.repository.UserSessionRepository;
import com.aiagent.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing user sessions.
 * Tracks login/logout events, supports multi-device session management,
 * and provides session validation and cleanup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository sessionRepository;

    /** Default session timeout: 24 hours */
    private static final long DEFAULT_SESSION_TIMEOUT_HOURS = 24;

    // User-Agent parsing patterns
    private static final Pattern BROWSER_PATTERN = Pattern.compile(
            "(Chrome|Firefox|Safari|Edge|Opera|MSIE|Trident)[/\\s](\\d+[.\\d]*)");
    private static final Pattern OS_PATTERN = Pattern.compile(
            "(Windows NT \\d+[.\\d]*|Mac OS X \\d+[._\\d]*|Linux|Android \\d+[.\\d]*|iPhone OS \\d+[._\\d]*|iPad)");

    /**
     * Create a new session record when a user logs in.
     *
     * @param userId    user ID
     * @param username  username
     * @param sessionId JWT session identifier (jti claim or token hash)
     * @param request   HTTP request for extracting client info
     * @return the created UserSession entity
     */
    @Transactional(rollbackFor = Exception.class)
    public UserSession createSession(Long userId, String username, String sessionId, HttpServletRequest request) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setUsername(username);
        session.setSessionId(sessionId);
        session.setIpAddress(SecurityUtils.getClientIp(request));
        session.setUserAgent(truncateUserAgent(request.getHeader("User-Agent")));
        session.setBrowser(parseBrowser(request.getHeader("User-Agent")));
        session.setOs(parseOs(request.getHeader("User-Agent")));
        session.setLoginTime(LocalDateTime.now());
        session.setLastAccessTime(LocalDateTime.now());
        session.setExpireTime(LocalDateTime.now().plusHours(DEFAULT_SESSION_TIMEOUT_HOURS));
        session.setStatus(SessionStatus.ACTIVE);

        session = sessionRepository.save(session);
        log.info("Session created: userId={}, sessionId={}, ip={}", userId, sessionId, session.getIpAddress());
        return session;
    }

    /**
     * Refresh a session's last access time and expiration.
     *
     * @param sessionId session identifier
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshSession(String sessionId) {
        Optional<UserSession> optional = sessionRepository.findBySessionId(sessionId);
        if (optional.isPresent()) {
            UserSession session = optional.get();
            if (session.getStatus() == SessionStatus.ACTIVE) {
                session.setLastAccessTime(LocalDateTime.now());
                session.setExpireTime(LocalDateTime.now().plusHours(DEFAULT_SESSION_TIMEOUT_HOURS));
                sessionRepository.save(session);
            }
        }
    }

    /**
     * Invalidate a specific session (user logout).
     *
     * @param sessionId session identifier to invalidate
     */
    @Transactional(rollbackFor = Exception.class)
    public void invalidateSession(String sessionId) {
        Optional<UserSession> optional = sessionRepository.findBySessionId(sessionId);
        if (optional.isPresent()) {
            UserSession session = optional.get();
            session.setStatus(SessionStatus.LOGOUT);
            sessionRepository.save(session);
            log.info("Session invalidated (logout): userId={}, sessionId={}", session.getUserId(), sessionId);
        }
    }

    /**
     * Invalidate all active sessions for a user (kick from all devices).
     *
     * @param userId user ID
     * @return number of sessions invalidated
     */
    @Transactional(rollbackFor = Exception.class)
    public int invalidateAllUserSessions(Long userId) {
        int count = sessionRepository.invalidateAllUserSessions(userId);
        log.info("All sessions invalidated for user: userId={}, count={}", userId, count);
        return count;
    }

    /**
     * Kick a specific session.
     *
     * @param sessionId session identifier to kick
     */
    @Transactional(rollbackFor = Exception.class)
    public void kickSession(String sessionId) {
        Optional<UserSession> optional = sessionRepository.findBySessionId(sessionId);
        if (optional.isPresent()) {
            UserSession session = optional.get();
            session.setStatus(SessionStatus.KICKED);
            sessionRepository.save(session);
            log.info("Session kicked: userId={}, sessionId={}", session.getUserId(), sessionId);
        }
    }

    /**
     * Get all currently active (online) sessions.
     *
     * @return list of active sessions
     */
    public List<UserSession> getOnlineSessions() {
        return sessionRepository.findByStatus(SessionStatus.ACTIVE);
    }

    /**
     * Get all sessions for a specific user.
     *
     * @param userId user ID
     * @return list of user sessions
     */
    public List<UserSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserId(userId);
    }

    /**
     * Get all active sessions for a specific user.
     *
     * @param userId user ID
     * @return list of active user sessions
     */
    public List<UserSession> getActiveUserSessions(Long userId) {
        return sessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE);
    }

    /**
     * Check if a session is valid (exists, is ACTIVE, and not expired).
     *
     * @param sessionId session identifier
     * @return true if the session is valid
     */
    public boolean isSessionValid(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        Optional<UserSession> optional = sessionRepository.findBySessionId(sessionId);
        if (optional.isEmpty()) {
            return false;
        }
        UserSession session = optional.get();
        return session.getStatus() == SessionStatus.ACTIVE
                && session.getExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * Scheduled task to clean up expired sessions.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredSessions() {
        List<UserSession> expiredSessions = sessionRepository.findExpiredActiveSessions(LocalDateTime.now());
        if (!expiredSessions.isEmpty()) {
            for (UserSession session : expiredSessions) {
                session.setStatus(SessionStatus.EXPIRED);
            }
            sessionRepository.saveAll(expiredSessions);
            log.info("Cleaned up {} expired sessions", expiredSessions.size());
        }

        // Delete old non-active sessions older than 30 days
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        int deleted = sessionRepository.deleteInactiveSessionsBefore(threshold);
        if (deleted > 0) {
            log.info("Deleted {} old inactive sessions", deleted);
        }
    }

    /**
     * Get the count of currently online users (users with at least one active session).
     *
     * @return count of online users
     */
    public long getOnlineUserCount() {
        return sessionRepository.countByStatus(SessionStatus.ACTIVE);
    }

    // ==================== Private Helpers ====================

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        Matcher matcher = BROWSER_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2);
        }
        return "Unknown";
    }

    private String parseOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        Matcher matcher = OS_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group(1).replace("_", ".");
        }
        return "Unknown";
    }
}
