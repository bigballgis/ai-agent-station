package com.aiagent.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Typed WebSocket event DTO for real-time notifications.
 * Provides structured event types for different domain events.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Typed event classification */
    public enum EventType {
        /** Agent execution started, completed, or failed */
        AGENT_STATUS_CHANGED,
        /** Workflow instance status update */
        WORKFLOW_STATUS_CHANGED,
        /** New alert triggered */
        ALERT_FIRED,
        /** Approval request waiting for action */
        APPROVAL_PENDING,
        /** General system notification */
        SYSTEM_NOTIFICATION
    }

    private EventType eventType;
    private String title;
    private String content;
    private LocalDateTime timestamp;
    private Map<String, Object> payload;

    public WebSocketEventDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketEventDTO(EventType eventType, String title, String content, Map<String, Object> payload) {
        this.eventType = eventType;
        this.title = title;
        this.content = content;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    // ==================== Static Factory Methods ====================

    /**
     * Create an AGENT_STATUS_CHANGED event.
     */
    public static WebSocketEventDTO agentStatusChanged(String agentName, String status, Map<String, Object> extra) {
        Map<String, Object> payload = Map.of(
                "agentName", agentName,
                "status", status
        );
        if (extra != null) {
            payload = mergeMaps(payload, extra);
        }
        return new WebSocketEventDTO(
                EventType.AGENT_STATUS_CHANGED,
                "Agent: " + agentName,
                "Agent status changed to " + status,
                payload
        );
    }

    /**
     * Create a WORKFLOW_STATUS_CHANGED event.
     */
    public static WebSocketEventDTO workflowStatusChanged(String workflowName, Long instanceId, String status) {
        return new WebSocketEventDTO(
                EventType.WORKFLOW_STATUS_CHANGED,
                "Workflow: " + workflowName,
                "Instance #" + instanceId + " status changed to " + status,
                Map.of("workflowName", workflowName, "instanceId", instanceId, "status", status)
        );
    }

    /**
     * Create an ALERT_FIRED event.
     */
    public static WebSocketEventDTO alertFired(String ruleName, String severity, String message) {
        return new WebSocketEventDTO(
                EventType.ALERT_FIRED,
                "Alert: " + ruleName,
                message,
                Map.of("ruleName", ruleName, "severity", severity)
        );
    }

    /**
     * Create an APPROVAL_PENDING event.
     */
    public static WebSocketEventDTO approvalPending(String agentName, Long approvalId, String submitter) {
        return new WebSocketEventDTO(
                EventType.APPROVAL_PENDING,
                "Approval Required: " + agentName,
                "Agent '" + agentName + "' submitted by " + submitter + " is waiting for approval",
                Map.of("agentName", agentName, "approvalId", approvalId, "submitter", submitter)
        );
    }

    /**
     * Create a SYSTEM_NOTIFICATION event.
     */
    public static WebSocketEventDTO systemNotification(String title, String content, Map<String, Object> payload) {
        return new WebSocketEventDTO(
                EventType.SYSTEM_NOTIFICATION,
                title,
                content,
                payload
        );
    }

    // ==================== Getters and Setters ====================

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    // ==================== Private Helpers ====================

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mergeMaps(Map<String, Object> base, Map<String, Object> extra) {
        java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>(base);
        merged.putAll(extra);
        return merged;
    }
}
