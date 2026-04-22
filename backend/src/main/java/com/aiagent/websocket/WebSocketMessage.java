package com.aiagent.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket message DTO used for real-time notifications, alerts, workflow updates, and system messages.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Message type classification */
    public enum MessageType {
        NOTIFICATION,
        ALERT,
        WORKFLOW,
        SYSTEM
    }

    /** Message severity level */
    public enum MessageLevel {
        INFO,
        WARNING,
        ERROR,
        SUCCESS
    }

    private MessageType type;
    private String title;
    private String content;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
    private MessageLevel level;

    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketMessage(MessageType type, String title, String content,
                            Map<String, Object> data, MessageLevel level) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.data = data;
        this.level = level;
        this.timestamp = LocalDateTime.now();
    }

    // ==================== Static Factory Methods ====================

    public static WebSocketMessage notification(String title, String content) {
        return new WebSocketMessage(MessageType.NOTIFICATION, title, content, null, MessageLevel.INFO);
    }

    public static WebSocketMessage notification(String title, String content, Map<String, Object> data) {
        return new WebSocketMessage(MessageType.NOTIFICATION, title, content, data, MessageLevel.INFO);
    }

    public static WebSocketMessage alert(String title, String content, MessageLevel level) {
        return new WebSocketMessage(MessageType.ALERT, title, content, null, level);
    }

    public static WebSocketMessage alert(String title, String content, Map<String, Object> data, MessageLevel level) {
        return new WebSocketMessage(MessageType.ALERT, title, content, data, level);
    }

    public static WebSocketMessage workflow(String title, String content, Map<String, Object> data) {
        return new WebSocketMessage(MessageType.WORKFLOW, title, content, data, MessageLevel.INFO);
    }

    public static WebSocketMessage system(String title, String content) {
        return new WebSocketMessage(MessageType.SYSTEM, title, content, null, MessageLevel.INFO);
    }

    public static WebSocketMessage system(String title, String content, MessageLevel level) {
        return new WebSocketMessage(MessageType.SYSTEM, title, content, null, level);
    }

    // ==================== Getters and Setters ====================

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
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

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public MessageLevel getLevel() {
        return level;
    }

    public void setLevel(MessageLevel level) {
        this.level = level;
    }
}
