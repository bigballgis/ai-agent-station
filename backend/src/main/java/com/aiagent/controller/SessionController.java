package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.Result;
import com.aiagent.entity.UserSession;
import com.aiagent.service.SessionService;
import com.aiagent.util.SecurityUtils;
import com.aiagent.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for session management.
 * Provides endpoints to view online sessions, kick sessions, and manage multi-device access.
 */
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "会话管理", description = "会话管理接口")
public class SessionController {

    private final SessionService sessionService;

    /**
     * Get all currently online sessions.
     *
     * @return list of active sessions with user info
     */
    @RequiresPermission("session:manage")
    @GetMapping("/online")
    public Result<List<SessionVO>> getOnlineSessions() {
        List<SessionVO> voList = sessionService.getOnlineSessions().stream()
                .map(SessionVO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * Get the current user's sessions (all devices).
     *
     * @return list of the current user's sessions
     */
    @Operation(summary = "获取在线会话列表")
    @RequiresPermission("session:manage")
    @GetMapping("/me")
    public Result<List<SessionVO>> getMySessions() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<SessionVO> voList = sessionService.getUserSessions(currentUserId).stream()
                .map(SessionVO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * Kick a specific session by session ID.
     *
     * @param sessionId the session to kick
     * @return success result
     */
    @Operation(summary = "获取当前用户的会话列表")
    @RequiresPermission("session:manage")
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
    @Operation(summary = "踢出指定会话")
    @RequiresPermission("session:manage")
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
    @Operation(summary = "踢出用户所有设备")
    @RequiresPermission("session:manage")
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
