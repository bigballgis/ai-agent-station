package com.aiagent.service;

import com.aiagent.entity.UserSession;
import com.aiagent.entity.UserSession.SessionStatus;
import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SessionService 单元测试
 * 测试会话创建、失效、验证、清理等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("会话服务测试")
class SessionServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private AiAgentProperties aiAgentProperties;

    @InjectMocks
    private SessionService sessionService;

    private UserSession testSession;

    @BeforeEach
    void setUp() {
        testSession = new UserSession();
        testSession.setId(1L);
        testSession.setUserId(1L);
        testSession.setUsername("testuser");
        testSession.setSessionId("session-abc-123");
        testSession.setIpAddress("192.168.1.1");
        testSession.setBrowser("Chrome 120");
        testSession.setOs("Windows NT 10.0");
        testSession.setLoginTime(LocalDateTime.now());
        testSession.setLastAccessTime(LocalDateTime.now());
        testSession.setExpireTime(LocalDateTime.now().plusHours(24));
        testSession.setStatus(SessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("创建会话 - 成功")
    void testCreateSession_Success() {
        AiAgentProperties.Session sessionProps = new AiAgentProperties.Session();
        sessionProps.setTimeoutHours(24);
        sessionProps.setMaxConcurrentSessions(3);
        when(aiAgentProperties.getSession()).thenReturn(sessionProps);

        // Mock concurrent session limit check: user has no active sessions
        when(sessionRepository.findByUserIdAndStatus(1L, SessionStatus.ACTIVE)).thenReturn(List.of());
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> {
            UserSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        org.springframework.mock.web.MockHttpServletRequest mockRequest = new org.springframework.mock.web.MockHttpServletRequest();
        mockRequest.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");
        mockRequest.setRemoteAddr("192.168.1.1");
        try {
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                    new org.springframework.web.context.request.ServletRequestAttributes(mockRequest)
            );

            UserSession created = sessionService.createSession(1L, "testuser", "session-abc-123");

            assertNotNull(created);
            assertEquals(1L, created.getUserId());
            assertEquals("session-abc-123", created.getSessionId());
            assertEquals(SessionStatus.ACTIVE, created.getStatus());
            assertEquals("192.168.1.1", created.getIpAddress());
            verify(sessionRepository).save(any(UserSession.class));
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @DisplayName("失效会话 - 成功")
    void testInvalidateSession_Success() {
        when(sessionRepository.findBySessionId("session-abc-123")).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.invalidateSession("session-abc-123");

        assertEquals(SessionStatus.LOGOUT, testSession.getStatus());
        verify(sessionRepository).save(testSession);
    }

    @Test
    @DisplayName("失效会话 - 会话不存在不抛异常")
    void testInvalidateSession_NotFound() {
        when(sessionRepository.findBySessionId("nonexistent")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> {
            sessionService.invalidateSession("nonexistent");
        });
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("验证会话 - 有效会话返回true")
    void testIsSessionValid_ActiveSession() {
        when(sessionRepository.findBySessionId("session-abc-123")).thenReturn(Optional.of(testSession));

        assertTrue(sessionService.isSessionValid("session-abc-123"));
    }

    @Test
    @DisplayName("验证会话 - 已过期会话返回false")
    void testIsSessionValid_ExpiredSession() {
        testSession.setExpireTime(LocalDateTime.now().minusHours(1));
        when(sessionRepository.findBySessionId("session-abc-123")).thenReturn(Optional.of(testSession));

        assertFalse(sessionService.isSessionValid("session-abc-123"));
    }

    @Test
    @DisplayName("验证会话 - 已登出会话返回false")
    void testIsSessionValid_LoggedOutSession() {
        testSession.setStatus(SessionStatus.LOGOUT);
        when(sessionRepository.findBySessionId("session-abc-123")).thenReturn(Optional.of(testSession));

        assertFalse(sessionService.isSessionValid("session-abc-123"));
    }

    @Test
    @DisplayName("验证会话 - 空sessionId返回false")
    void testIsSessionValid_NullSessionId() {
        assertFalse(sessionService.isSessionValid(null));
        assertFalse(sessionService.isSessionValid(""));
        assertFalse(sessionService.isSessionValid("   "));
    }

    @Test
    @DisplayName("验证会话 - 不存在的会话返回false")
    void testIsSessionValid_NotFound() {
        when(sessionRepository.findBySessionId("nonexistent")).thenReturn(Optional.empty());

        assertFalse(sessionService.isSessionValid("nonexistent"));
    }

    @Test
    @DisplayName("清理过期会话 - 成功")
    void testCleanupExpiredSessions_Success() {
        UserSession expiredSession = new UserSession();
        expiredSession.setId(2L);
        expiredSession.setUserId(2L);
        expiredSession.setSessionId("expired-session");
        expiredSession.setStatus(SessionStatus.ACTIVE);
        expiredSession.setExpireTime(LocalDateTime.now().minusHours(1));

        when(sessionRepository.findExpiredActiveSessions(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredSession));
        when(sessionRepository.saveAll(anyList())).thenReturn(List.of(expiredSession));
        when(sessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(5);

        sessionService.cleanupExpiredSessions();

        assertEquals(SessionStatus.EXPIRED, expiredSession.getStatus());
        verify(sessionRepository).saveAll(anyList());
        verify(sessionRepository).deleteInactiveSessionsBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("清理过期会话 - 无过期会话")
    void testCleanupExpiredSessions_NoExpiredSessions() {
        when(sessionRepository.findExpiredActiveSessions(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(sessionRepository.deleteInactiveSessionsBefore(any(LocalDateTime.class))).thenReturn(0);

        sessionService.cleanupExpiredSessions();

        verify(sessionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("踢出会话 - 成功")
    void testKickSession_Success() {
        when(sessionRepository.findBySessionId("session-abc-123")).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.kickSession("session-abc-123");

        assertEquals(SessionStatus.KICKED, testSession.getStatus());
        verify(sessionRepository).save(testSession);
    }

    @Test
    @DisplayName("获取在线会话 - 成功")
    void testGetOnlineSessions_Success() {
        when(sessionRepository.findByStatus(SessionStatus.ACTIVE))
                .thenReturn(List.of(testSession));

        List<UserSession> sessions = sessionService.getOnlineSessions();

        assertNotNull(sessions);
        assertEquals(1, sessions.size());
        assertEquals(SessionStatus.ACTIVE, sessions.get(0).getStatus());
    }

    @Test
    @DisplayName("失效用户所有会话 - 成功")
    void testInvalidateAllUserSessions_Success() {
        when(sessionRepository.invalidateAllUserSessions(1L)).thenReturn(3);

        int count = sessionService.invalidateAllUserSessions(1L);

        assertEquals(3, count);
        verify(sessionRepository).invalidateAllUserSessions(1L);
    }

    @Test
    @DisplayName("并发会话限制 - 超过3个活跃会话时踢出最早的")
    void testEnforceConcurrentSessionLimit_EvictsOldest() {
        // Create 3 existing active sessions
        UserSession session1 = new UserSession();
        session1.setId(1L);
        session1.setUserId(1L);
        session1.setSessionId("session-1");
        session1.setStatus(SessionStatus.ACTIVE);
        session1.setLoginTime(LocalDateTime.now().minusHours(3));

        UserSession session2 = new UserSession();
        session2.setId(2L);
        session2.setUserId(1L);
        session2.setSessionId("session-2");
        session2.setStatus(SessionStatus.ACTIVE);
        session2.setLoginTime(LocalDateTime.now().minusHours(2));

        UserSession session3 = new UserSession();
        session3.setId(3L);
        session3.setUserId(1L);
        session3.setSessionId("session-3");
        session3.setStatus(SessionStatus.ACTIVE);
        session3.setLoginTime(LocalDateTime.now().minusHours(1));

        when(sessionRepository.findByUserIdAndStatus(1L, SessionStatus.ACTIVE))
                .thenReturn(List.of(session1, session2, session3));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Enforce limit - should evict the oldest session
        int evicted = sessionService.enforceConcurrentSessionLimit(1L);

        assertEquals(1, evicted);
        assertEquals(SessionStatus.KICKED, session1.getStatus());
        verify(sessionRepository).save(session1);
    }

    @Test
    @DisplayName("并发会话限制 - 未超过限制时不踢出")
    void testEnforceConcurrentSessionLimit_UnderLimit() {
        when(sessionRepository.findByUserIdAndStatus(1L, SessionStatus.ACTIVE))
                .thenReturn(List.of(testSession));

        int evicted = sessionService.enforceConcurrentSessionLimit(1L);

        assertEquals(0, evicted);
        verify(sessionRepository, never()).save(any(UserSession.class));
    }
}
