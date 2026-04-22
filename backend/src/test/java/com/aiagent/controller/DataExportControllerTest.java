package com.aiagent.controller;

import com.aiagent.service.DataExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DataExportController 单元测试
 * 使用 MockMvc 测试数据导出接口
 */
@WebMvcTest(DataExportController.class)
@DisplayName("数据导出控制器测试")
class DataExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataExportService dataExportService;

    @Test
    @WithMockUser
    @DisplayName("导出Agent数据 - 成功返回200")
    void testExportAgents() throws Exception {
        doNothing().when(dataExportService).exportAgents(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));

        mockMvc.perform(get("/export/agents"))
                .andExpect(status().isOk());

        verify(dataExportService).exportAgents(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser
    @DisplayName("导出Agent数据 - 带过滤参数")
    void testExportAgents_WithFilters() throws Exception {
        doNothing().when(dataExportService).exportAgents(
                any(), any(), any(), any(), any(), any(HttpServletResponse.class));

        mockMvc.perform(get("/export/agents")
                        .param("tenantId", "100")
                        .param("status", "PUBLISHED")
                        .param("keyword", "测试"))
                .andExpect(status().isOk());

        verify(dataExportService).exportAgents(
                any(), any(), any(), any(), any(), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser
    @DisplayName("导出用户数据 - 成功返回200")
    void testExportUsers() throws Exception {
        doNothing().when(dataExportService).exportUsers(
                isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));

        mockMvc.perform(get("/export/users"))
                .andExpect(status().isOk());

        verify(dataExportService).exportUsers(
                isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser
    @DisplayName("导出用户数据 - 带过滤参数")
    void testExportUsers_WithFilters() throws Exception {
        doNothing().when(dataExportService).exportUsers(
                any(), any(), any(), any(), any(HttpServletResponse.class));

        mockMvc.perform(get("/export/users")
                        .param("tenantId", "100")
                        .param("keyword", "admin"))
                .andExpect(status().isOk());

        verify(dataExportService).exportUsers(
                any(), any(), any(), any(), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser
    @DisplayName("导出系统日志 - 成功返回200")
    void testExportLogs() throws Exception {
        doNothing().when(dataExportService).exportLogs(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));

        mockMvc.perform(get("/export/logs"))
                .andExpect(status().isOk());

        verify(dataExportService).exportLogs(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser
    @DisplayName("导出测试结果 - 成功返回200")
    void testExportTestResults() throws Exception {
        doNothing().when(dataExportService).exportTestResults(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));

        mockMvc.perform(get("/export/test-results"))
                .andExpect(status().isOk());

        verify(dataExportService).exportTestResults(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));
    }

    @Test
    @WithMockUser
    @DisplayName("导出工作流实例 - 成功返回200")
    void testExportWorkflowInstances() throws Exception {
        doNothing().when(dataExportService).exportWorkflowInstances(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));

        mockMvc.perform(get("/export/workflow-instances"))
                .andExpect(status().isOk());

        verify(dataExportService).exportWorkflowInstances(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(HttpServletResponse.class));
    }
}
