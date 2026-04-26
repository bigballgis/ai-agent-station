package com.aiagent.controller;

import com.aiagent.entity.UserSession;
import com.aiagent.entity.UserSession.SessionStatus;
import com.aiagent.service.SessionService;
import com.aiagent.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SessionController 单元测试
 * 使用 MockMvc 测试会话管理接口
 */
@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("会话控制器测试")
class SessionControllerTest extends AbstractWebMvcSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
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
    @WithMockUser
    @DisplayName("获取在线会话列表 - 成功返回200")
    void testGetOnlineSessions() throws Exception {
        when(sessionService.getOnlineSessions()).thenReturn(List.of(testSession));

        mockMvc.perform(get("/v1/sessions/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].username").value("testuser"));
    }

    @Test
    @WithMockUser
    @DisplayName("获取在线会话列表 - 无在线会话")
    void testGetOnlineSessions_Empty() throws Exception {
        when(sessionService.getOnlineSessions()).thenReturn(List.of());

        mockMvc.perform(get("/v1/sessions/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("踢出指定会话 - 成功返回200")
    void testKickSession() throws Exception {
        doNothing().when(sessionService).kickSession("session-abc-123");

        mockMvc.perform(delete("/v1/sessions/session-abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sessionService).kickSession("session-abc-123");
    }

    @Test
    @WithMockUser
    @DisplayName("踢出用户所有设备 - 成功返回200")
    void testKickUserFromAllDevices() throws Exception {
        when(sessionService.invalidateAllUserSessions(1L)).thenReturn(3);

        mockMvc.perform(delete("/v1/sessions/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.kickedSessions").value(3));

        verify(sessionService).invalidateAllUserSessions(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("获取会话统计信息 - 成功返回200")
    void testGetSessionStats() throws Exception {
        UserSession session2 = new UserSession();
        session2.setId(2L);
        session2.setUserId(2L);
        session2.setSessionId("session-def-456");
        session2.setOs("Mac OS X");
        session2.setStatus(SessionStatus.ACTIVE);

        when(sessionService.getOnlineUserCount()).thenReturn(2L);
        when(sessionService.getOnlineSessions()).thenReturn(List.of(testSession, session2));

        mockMvc.perform(get("/v1/sessions/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.onlineUserCount").value(2))
                .andExpect(jsonPath("$.data.totalActiveSessions").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("获取当前用户会话列表 - 成功返回200")
    void testGetMySessions() throws Exception {
        try (var mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(sessionService.getUserSessions(1L)).thenReturn(List.of(testSession));

            mockMvc.perform(get("/v1/sessions/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.length()").value(1));
        }
    }
}
