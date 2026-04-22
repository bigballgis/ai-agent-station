package com.aiagent.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * WebSocket handler for real-time notifications.
 * Maintains sessions keyed by userId, supports per-user, broadcast, and per-tenant messaging.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final ObjectMapper objectMapper;

    /** userId -> Set of WebSocketSession (a user may connect from multiple devices) */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /** sessionId -> userId (reverse lookup) */
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    /** tenantId -> Set of userIds currently online in that tenant */
    private final ConcurrentHashMap<Long, Set<Long>> tenantOnlineUsers = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // ==================== Connection Lifecycle ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        Long tenantId = extractTenantId(session);

        if (userId == null) {
            log.warn("WebSocket connection rejected: no userId provided. sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("Missing userId parameter"));
            return;
        }

        // Register session
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionUserMap.put(session.getId(), userId);

        // Track tenant membership
        if (tenantId != null) {
            tenantOnlineUsers.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }

        log.info("WebSocket connected: userId={}, tenantId={}, sessionId={}", userId, tenantId, session.getId());

        // Send welcome message
        WebSocketMessage welcome = new WebSocketMessage(
                WebSocketMessage.MessageType.SYSTEM,
                "Connected",
                "WebSocket connection established successfully",
                null,
                WebSocketMessage.MessageLevel.INFO
        );
        sendMessage(session, welcome);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // Handle ping/pong heartbeat
        if ("ping".equalsIgnoreCase(payload.trim())) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        // Handle other message types if needed
        try {
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            log.debug("Received WebSocket message from userId={}: type={}", sessionUserMap.get(session.getId()), wsMessage.getType());
        } catch (JsonProcessingException e) {
            log.debug("Received non-JSON WebSocket message: {}", payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    // Remove from tenant tracking
                    tenantOnlineUsers.forEach((tenantId, users) -> users.remove(userId));
                }
            }
        }
        log.info("WebSocket disconnected: userId={}, sessionId={}, status={}", userId, session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = sessionUserMap.get(session.getId());
        log.error("WebSocket transport error: userId={}, sessionId={}, error={}",
                userId, session.getId(), exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    // ==================== Public API ====================

    /**
     * Send a message to a specific user (all their connected sessions).
     *
     * @param userId  target user ID
     * @param message WebSocket message to send
     * @return true if at least one session received the message
     */
    public boolean sendToUser(Long userId, WebSocketMessage message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        boolean sent = false;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sent |= sendMessage(session, message);
            }
        }
        return sent;
    }

    /**
     * Broadcast a message to all connected users.
     *
     * @param message WebSocket message to broadcast
     */
    public void sendToAll(WebSocketMessage message) {
        for (Set<WebSocketSession> sessions : userSessions.values()) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
        log.info("Broadcast message sent to {} users, type={}", userSessions.size(), message.getType());
    }

    /**
     * Send a message to all users belonging to a specific tenant.
     *
     * @param tenantId target tenant ID
     * @param message  WebSocket message to send
     */
    public void sendToTenant(Long tenantId, WebSocketMessage message) {
        Set<Long> onlineUserIds = tenantOnlineUsers.get(tenantId);
        if (onlineUserIds == null || onlineUserIds.isEmpty()) {
            return;
        }
        for (Long userId : onlineUserIds) {
            sendToUser(userId, message);
        }
        log.info("Tenant broadcast sent to tenantId={}, onlineUsers={}, type={}",
                tenantId, onlineUserIds.size(), message.getType());
    }

    /**
     * Check if a user is currently online (has at least one active WebSocket session).
     */
    public boolean isUserOnline(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        return sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    /**
     * Get the set of currently online user IDs.
     */
    public Set<Long> getOnlineUserIds() {
        return userSessions.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(WebSocketSession::isOpen))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Get the total number of active WebSocket connections.
     */
    public int getActiveConnectionCount() {
        return sessionUserMap.size();
    }

    // ==================== Private Helpers ====================

    private boolean sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                    return true;
                }
            }
        } catch (IOException e) {
            log.error("Failed to send WebSocket message to session {}: {}",
                    session.getId(), e.getMessage());
        }
        return false;
    }

    private Long extractUserId(WebSocketSession session) {
        try {
            Map<String, String> params = UriComponentsBuilder
                    .fromUri(session.getUri())
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            String userIdStr = params.get("userId");
            if (userIdStr != null) {
                return Long.parseLong(userIdStr);
            }
        } catch (Exception e) {
            log.warn("Failed to extract userId from WebSocket URI: {}", e.getMessage());
        }
        return null;
    }

    private Long extractTenantId(WebSocketSession session) {
        try {
            Map<String, String> params = UriComponentsBuilder
                    .fromUri(session.getUri())
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            String tenantIdStr = params.get("tenantId");
            if (tenantIdStr != null) {
                return Long.parseLong(tenantIdStr);
            }
        } catch (Exception e) {
            log.debug("Failed to extract tenantId from WebSocket URI: {}", e.getMessage());
        }
        return null;
    }
}
