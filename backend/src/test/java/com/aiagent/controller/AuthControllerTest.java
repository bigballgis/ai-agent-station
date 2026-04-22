package com.aiagent.controller;

import com.aiagent.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 单元测试
 * 使用 MockMvc 测试认证接口
 */
@WebMvcTest(AuthController.class)
@DisplayName("认证控制器测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("登录 - 成功返回200")
    void testLogin_Success() throws Exception {
        Map<String, Object> loginResult = Map.of(
                "token", "test_access_token",
                "refreshToken", "test_refresh_token",
                "user", Map.of("id", 1, "username", "admin")
        );

        when(authService.login(eq("admin"), eq("password123"), isNull()))
                .thenReturn(loginResult);

        String json = objectMapper.writeValueAsString(Map.of(
                "username", "admin",
                "password", "password123"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("test_access_token"))
                .andExpect(jsonPath("$.data.refreshToken").value("test_refresh_token"));
    }

    @Test
    @DisplayName("登录 - 用户名为空返回400")
    void testLogin_Fail_EmptyUsername() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "password", "password123"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("登录 - 密码为空返回400")
    void testLogin_Fail_EmptyPassword() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "username", "admin"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("登录 - 用户名过短返回400")
    void testLogin_Fail_ShortUsername() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "username", "ab",
                "password", "password123"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("刷新Token - 成功返回200")
    void testRefreshToken() throws Exception {
        Map<String, Object> refreshResult = Map.of(
                "token", "new_access_token",
                "refreshToken", "new_refresh_token"
        );

        when(authService.refreshToken("valid_refresh_token")).thenReturn(refreshResult);

        String json = objectMapper.writeValueAsString(Map.of(
                "refreshToken", "valid_refresh_token"
        ));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("new_access_token"));
    }

    @Test
    @DisplayName("刷新Token - refreshToken为空返回400")
    void testRefreshToken_EmptyToken() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("登出 - 成功返回200")
    void testLogout() throws Exception {
        doNothing().when(authService).logout(anyLong());

        mockMvc.perform(post("/auth/logout")
                        .header("X-User-ID", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).logout(1L);
    }

    @Test
    @DisplayName("登出 - 无X-User-ID头不调用logout")
    void testLogout_NoUserId() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService, never()).logout(anyLong());
    }
}
