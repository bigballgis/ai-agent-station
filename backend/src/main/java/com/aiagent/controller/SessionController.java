package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.entity.UserSession;
import com.aiagent.service.SessionService;
import com.aiagent.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for session management.
 * Provides endpoints to view online sessions, kick sessions, and manage multi-device access.
 */
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * Get all currently online sessions.
     *
     * @return list of active sessions with user info
     */
    @GetMapping("/online")
    public Result<List<UserSession>> getOnlineSessions() {
        List<UserSession> sessions = sessionService.getOnlineSessions();
        return Result.success(sessions);
    }

    /**
     * Get the current user's sessions (all devices).
     *
     * @return list of the current user's sessions
     */
    @GetMapping("/me")
    public Result<List<UserSession>> getMySessions() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<UserSession> sessions = sessionService.getUserSessions(currentUserId);
        return Result.success(sessions);
    }

    /**
     * Kick a specific session by session ID.
     *
     * @param sessionId the session to kick
     * @return success result
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> kickSession(@PathVariable String sessionId) {
        sessionService.kickSession(sessionId);
        return Result.success("Session kicked successfully", null);
    }

    /**
     * Kick a user from all devices (invalidate all active sessions).
     *
     * @param userId the user ID to kick
     * @return result with the number of sessions kicked
     */
    @DeleteMapping("/user/{userId}")
    public Result<Map<String, Object>> kickUserFromAllDevices(@PathVariable Long userId) {
        int count = sessionService.invalidateAllUserSessions(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("kickedSessions", count);
        return Result.success("User kicked from all devices", data);
    }

    /**
     * Get online user statistics.
     *
     * @return statistics about online sessions
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getSessionStats() {
        long onlineCount = sessionService.getOnlineUserCount();
        List<UserSession> onlineSessions = sessionService.getOnlineSessions();

        Map<String, Object> stats = new HashMap<>();
        stats.put("onlineUserCount", onlineCount);
        stats.put("totalActiveSessions", onlineSessions.size());

        // Group sessions by OS for a breakdown
        Map<String, Long> osBreakdown = onlineSessions.stream()
                .filter(s -> s.getOs() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        UserSession::getOs,
                        java.util.stream.Collectors.counting()
                ));
        stats.put("osBreakdown", osBreakdown);

        return Result.success(stats);
    }
}
