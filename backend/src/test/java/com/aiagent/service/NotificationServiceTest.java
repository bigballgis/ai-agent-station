package com.aiagent.service;

import com.aiagent.websocket.NotificationWebSocketHandler;
import com.aiagent.websocket.WebSocketMessage;
import com.aiagent.websocket.WebSocketMessage.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationService 单元测试
 * 测试通知发送、广播、租户通知等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知服务测试")
class NotificationServiceTest {

    @Mock
    private NotificationWebSocketHandler webSocketHandler;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("发送通知给指定用户 - 用户在线")
    void testSendNotification_UserOnline() {
        when(webSocketHandler.sendToUser(eq(1L), any(WebSocketMessage.class))).thenReturn(true);

        notificationService.sendNotification(1L, MessageType.NOTIFICATION, "测试标题", "测试内容", null);

        verify(webSocketHandler).sendToUser(eq(1L), any(WebSocketMessage.class));
    }

    @Test
    @DisplayName("发送通知给指定用户 - 用户离线不抛异常")
    void testSendNotification_UserOffline() {
        when(webSocketHandler.sendToUser(eq(1L), any(WebSocketMessage.class))).thenReturn(false);

        assertDoesNotThrow(() -> {
            notificationService.sendNotification(1L, MessageType.NOTIFICATION, "测试标题", "测试内容", null);
        });

        verify(webSocketHandler).sendToUser(eq(1L), any(WebSocketMessage.class));
    }

    @Test
    @DisplayName("发送WebSocketMessage给指定用户")
    void testSendNotification_WithMessage() {
        WebSocketMessage message = new WebSocketMessage(MessageType.ALERT, "告警", "告警内容",
                Map.of("key", "value"), WebSocketMessage.MessageLevel.WARNING);
        when(webSocketHandler.sendToUser(eq(1L), eq(message))).thenReturn(true);

        notificationService.sendNotification(1L, message);

        verify(webSocketHandler).sendToUser(eq(1L), eq(message));
    }

    @Test
    @DisplayName("广播通知 - 无额外数据")
    void testBroadcast_WithoutData() {
        notificationService.broadcast(MessageType.SYSTEM, "系统通知", "系统维护中");

        verify(webSocketHandler).sendToAll(argThat(msg ->
                msg.getType() == MessageType.SYSTEM &&
                msg.getTitle().equals("系统通知") &&
                msg.getContent().equals("系统维护中")
        ));
    }

    @Test
    @DisplayName("广播通知 - 带额外数据")
    void testBroadcast_WithData() {
        Map<String, Object> data = Map.of("key1", "value1", "key2", 123);
        notificationService.broadcast(MessageType.NOTIFICATION, "广播标题", "广播内容", data);

        verify(webSocketHandler).sendToAll(argThat(msg ->
                msg.getType() == MessageType.NOTIFICATION &&
                msg.getTitle().equals("广播标题") &&
                msg.getData() != null
        ));
    }

    @Test
    @DisplayName("发送租户通知 - 成功")
    void testSendToTenant_Success() {
        notificationService.sendToTenant(100L, MessageType.NOTIFICATION, "租户通知", "租户内容");

        verify(webSocketHandler).sendToTenant(eq(100L), argThat(msg ->
                msg.getType() == MessageType.NOTIFICATION &&
                msg.getTitle().equals("租户通知")
        ));
    }

    @Test
    @DisplayName("推送告警通知 - 成功")
    void testPushAlert_Success() {
        when(webSocketHandler.sendToUser(eq(1L), any(WebSocketMessage.class))).thenReturn(true);

        notificationService.pushAlert(1L, "告警标题", "告警内容",
                WebSocketMessage.MessageLevel.ERROR, Map.of("alertId", 42));

        verify(webSocketHandler).sendToUser(eq(1L), argThat(msg ->
                msg.getType() == MessageType.ALERT &&
                msg.getLevel() == WebSocketMessage.MessageLevel.ERROR
        ));
    }

    @Test
    @DisplayName("推送工作流状态通知 - 成功")
    void testPushWorkflowStatus_Success() {
        when(webSocketHandler.sendToUser(eq(1L), any(WebSocketMessage.class))).thenReturn(true);

        notificationService.pushWorkflowStatus(1L, "审批流程", 100L, "COMPLETED", null);

        verify(webSocketHandler).sendToUser(eq(1L), argThat(msg ->
                msg.getType() == MessageType.WORKFLOW &&
                msg.getTitle().contains("审批流程")
        ));
    }

    @Test
    @DisplayName("获取在线用户列表")
    void testGetOnlineUsers() {
        Set<Long> onlineUsers = Set.of(1L, 2L, 3L);
        when(webSocketHandler.getOnlineUserIds()).thenReturn(onlineUsers);

        Set<Long> result = notificationService.getOnlineUsers();

        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
    }

    @Test
    @DisplayName("检查用户是否在线 - 在线")
    void testIsUserOnline_Online() {
        when(webSocketHandler.isUserOnline(1L)).thenReturn(true);

        assertTrue(notificationService.isUserOnline(1L));
    }

    @Test
    @DisplayName("检查用户是否在线 - 离线")
    void testIsUserOnline_Offline() {
        when(webSocketHandler.isUserOnline(1L)).thenReturn(false);

        assertFalse(notificationService.isUserOnline(1L));
    }

    @Test
    @DisplayName("获取活跃连接数")
    void testGetActiveConnectionCount() {
        when(webSocketHandler.getActiveConnectionCount()).thenReturn(15);

        assertEquals(15, notificationService.getActiveConnectionCount());
    }
}
