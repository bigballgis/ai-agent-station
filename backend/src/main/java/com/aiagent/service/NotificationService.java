package com.aiagent.service;

import com.aiagent.websocket.NotificationWebSocketHandler;
import com.aiagent.websocket.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Service for sending real-time notifications via WebSocket.
 * Falls back to logging when the target user is offline.
 * Integrates with AlertService and WorkflowEngine for push notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationWebSocketHandler webSocketHandler;

    /**
     * Send a notification to a specific user.
     * If the user is offline, the notification is logged for later retrieval.
     *
     * @param userId  target user ID
     * @param type    message type (NOTIFICATION/ALERT/WORKFLOW/SYSTEM)
     * @param title   notification title
     * @param content notification content
     * @param data    optional extra payload
     */
    @Async
    public void sendNotification(Long userId, WebSocketMessage.MessageType type,
                                 String title, String content, Map<String, Object> data) {
        WebSocketMessage message = new WebSocketMessage(type, title, content, data,
                WebSocketMessage.MessageLevel.INFO);
        sendNotification(userId, message);
    }

    /**
     * Send a pre-built WebSocketMessage to a specific user.
     *
     * @param userId  target user ID
     * @param message the WebSocket message
     */
    @Async
    public void sendNotification(Long userId, WebSocketMessage message) {
        boolean sent = webSocketHandler.sendToUser(userId, message);
        if (!sent) {
            log.info("User {} is offline. Notification queued: type={}, title={}",
                    userId, message.getType(), message.getTitle());
            // In a production system, this would persist to a notification queue/table
            // for later delivery when the user comes back online.
        }
    }

    /**
     * Broadcast a notification to all connected users.
     *
     * @param type    message type
     * @param title   notification title
     * @param content notification content
     */
    @Async
    public void broadcast(WebSocketMessage.MessageType type, String title, String content) {
        WebSocketMessage message = new WebSocketMessage(type, title, content, null,
                WebSocketMessage.MessageLevel.INFO);
        webSocketHandler.sendToAll(message);
        log.info("Broadcast notification sent: type={}, title={}", type, title);
    }

    /**
     * Broadcast a notification to all connected users with extra data.
     *
     * @param type    message type
     * @param title   notification title
     * @param content notification content
     * @param data    extra payload
     */
    @Async
    public void broadcast(WebSocketMessage.MessageType type, String title, String content,
                          Map<String, Object> data) {
        WebSocketMessage message = new WebSocketMessage(type, title, content, data,
                WebSocketMessage.MessageLevel.INFO);
        webSocketHandler.sendToAll(message);
        log.info("Broadcast notification sent: type={}, title={}", type, title);
    }

    /**
     * Send a notification to all users in a specific tenant.
     *
     * @param tenantId target tenant ID
     * @param type     message type
     * @param title    notification title
     * @param content  notification content
     */
    @Async
    public void sendToTenant(Long tenantId, WebSocketMessage.MessageType type,
                             String title, String content) {
        WebSocketMessage message = new WebSocketMessage(type, title, content, null,
                WebSocketMessage.MessageLevel.INFO);
        webSocketHandler.sendToTenant(tenantId, message);
    }

    /**
     * Push an alert notification to a specific user.
     * Integrates with the alerting subsystem.
     *
     * @param userId  target user ID
     * @param title   alert title
     * @param content alert content
     * @param level   severity level
     * @param data    extra alert data
     */
    @Async
    public void pushAlert(Long userId, String title, String content,
                          WebSocketMessage.MessageLevel level, Map<String, Object> data) {
        WebSocketMessage message = WebSocketMessage.alert(title, content, data, level);
        sendNotification(userId, message);
    }

    /**
     * Push a workflow status change notification.
     * Integrates with the WorkflowEngine.
     *
     * @param userId       target user ID
     * @param workflowName workflow name
     * @param instanceId   workflow instance ID
     * @param status       new status
     * @param data         extra workflow data
     */
    @Async
    public void pushWorkflowStatus(Long userId, String workflowName, Long instanceId,
                                   String status, Map<String, Object> data) {
        String title = "Workflow: " + workflowName;
        String content = "Instance #" + instanceId + " status changed to " + status;
        if (data == null) {
            data = Map.of("instanceId", instanceId, "status", status, "workflowName", workflowName);
        } else {
            data.put("instanceId", instanceId);
            data.put("status", status);
            data.put("workflowName", workflowName);
        }
        WebSocketMessage message = WebSocketMessage.workflow(title, content, data);
        sendNotification(userId, message);
    }

    /**
     * Get the set of currently online user IDs.
     *
     * @return set of online user IDs
     */
    public Set<Long> getOnlineUsers() {
        return webSocketHandler.getOnlineUserIds();
    }

    /**
     * Check if a specific user is currently online.
     *
     * @param userId user ID to check
     * @return true if the user has at least one active WebSocket session
     */
    public boolean isUserOnline(Long userId) {
        return webSocketHandler.isUserOnline(userId);
    }

    /**
     * Get the total number of active WebSocket connections.
     *
     * @return active connection count
     */
    public int getActiveConnectionCount() {
        return webSocketHandler.getActiveConnectionCount();
    }
}
