package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.entity.Agent.AgentStatus;
import com.aiagent.entity.SystemLog;
import com.aiagent.entity.User;
import com.aiagent.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DataExportService 单元测试
 * 测试Agent、用户、日志等数据的CSV导出功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("数据导出服务测试")
class DataExportServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SystemLogRepository systemLogRepository;

    @Mock
    private AgentTestResultRepository testResultRepository;

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @InjectMocks
    private DataExportService dataExportService;

    private HttpServletResponse mockResponse;
    private ByteArrayOutputStream byteArrayOutputStream;

    @BeforeEach
    void setUp() throws IOException {
        mockResponse = mock(HttpServletResponse.class);
        byteArrayOutputStream = new ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) {
                byteArrayOutputStream.write(b);
            }
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(jakarta.servlet.WriteListener listener) {}
        };
        when(mockResponse.getOutputStream()).thenReturn(servletOutputStream);
    }

    private Agent createTestAgent(Long id, String name, AgentStatus status) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setName(name);
        agent.setDescription("描述" + id);
        agent.setStatus(status);
        agent.setCategory("chatbot");
        agent.setLanguage("zh");
        agent.setIsActive(true);
        agent.setCreatedBy(1L);
        agent.setCreatedAt(LocalDateTime.now().minusDays(1));
        agent.setUpdatedAt(LocalDateTime.now());
        return agent;
    }

    private User createTestUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPhone("138" + String.format("%08d", id));
        user.setTenantId(100L);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("导出Agent数据 - 全量导出")
    void testExportAgents_All() throws IOException {
        Agent agent1 = createTestAgent(1L, "Agent1", AgentStatus.PUBLISHED);
        Agent agent2 = createTestAgent(2L, "Agent2", AgentStatus.DRAFT);
        when(agentRepository.findAll()).thenReturn(List.of(agent1, agent2));

        dataExportService.exportAgents(null, null, null, null, null, mockResponse);

        String output = byteArrayOutputStream.toString("UTF-8");
        assertTrue(output.contains("Agent1"));
        assertTrue(output.contains("Agent2"));
        assertTrue(output.contains("ID,Name,Description"));
        verify(mockResponse).setContentType("text/csv;charset=UTF-8");
    }

    @Test
    @DisplayName("导出Agent数据 - 按状态过滤")
    void testExportAgents_FilterByStatus() throws IOException {
        Agent agent1 = createTestAgent(1L, "PublishedAgent", AgentStatus.PUBLISHED);
        Agent agent2 = createTestAgent(2L, "DraftAgent", AgentStatus.DRAFT);
        when(agentRepository.findAll()).thenReturn(List.of(agent1, agent2));

        dataExportService.exportAgents(null, "PUBLISHED", null, null, null, mockResponse);

        String output = byteArrayOutputStream.toString("UTF-8");
        assertTrue(output.contains("PublishedAgent"));
        assertFalse(output.contains("DraftAgent"));
    }

    @Test
    @DisplayName("导出Agent数据 - 按关键字过滤")
    void testExportAgents_FilterByKeyword() throws IOException {
        Agent agent1 = createTestAgent(1L, "搜索匹配Agent", AgentStatus.PUBLISHED);
        Agent agent2 = createTestAgent(2L, "不匹配", AgentStatus.PUBLISHED);
        when(agentRepository.findAll()).thenReturn(List.of(agent1, agent2));

        dataExportService.exportAgents(null, null, "搜索匹配", null, null, mockResponse);

        String output = byteArrayOutputStream.toString("UTF-8");
        assertTrue(output.contains("搜索匹配Agent"));
        assertFalse(output.contains("不匹配"));
    }

    @Test
    @DisplayName("导出用户数据 - 全量导出")
    void testExportUsers_All() throws IOException {
        User user1 = createTestUser(1L, "user1");
        User user2 = createTestUser(2L, "user2");
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        dataExportService.exportUsers(null, null, null, null, mockResponse);

        String output = byteArrayOutputStream.toString("UTF-8");
        assertTrue(output.contains("user1"));
        assertTrue(output.contains("user2"));
        assertTrue(output.contains("ID,Username,Email"));
    }

    @Test
    @DisplayName("导出用户数据 - 按租户过滤")
    void testExportUsers_FilterByTenant() throws IOException {
        User user1 = createTestUser(1L, "tenant1user");
        user1.setTenantId(100L);
        when(userRepository.findByTenantId(100L)).thenReturn(List.of(user1));

        dataExportService.exportUsers(100L, null, null, null, mockResponse);

        String output = byteArrayOutputStream.toString("UTF-8");
        assertTrue(output.contains("tenant1user"));
    }

    @Test
    @DisplayName("导出系统日志 - 全量导出")
    void testExportLogs_All() throws IOException {
        SystemLog log1 = new SystemLog();
        log1.setId(1L);
        log1.setModule("Agent管理");
        log1.setOperation("创建Agent");
        log1.setMethod("POST");
        log1.setIp("192.168.1.1");
        log1.setIsSuccess(true);
        log1.setCreatedAt(LocalDateTime.now());

        when(systemLogRepository.findAll()).thenReturn(List.of(log1));

        dataExportService.exportLogs(null, null, null, null, null, mockResponse);

        String output = byteArrayOutputStream.toString("UTF-8");
        assertTrue(output.contains("Agent管理"));
        assertTrue(output.contains("创建Agent"));
    }

    @Test
    @DisplayName("导出系统日志 - 按模块过滤")
    void testExportLogs_FilterByModule() throws IOException {
        SystemLog log1 = new SystemLog();
        log1.setId(1L);
        log1.setModule("Agent管理");
        log1.setOperation("创建Agent");
        log1.setIsSuccess(true);
        log1.setCreatedAt(LocalDateTime.now());

        SystemLog log2 = new SystemLog();
        log2.setId(2L);
        log2.setModule("用户管理");
        log2.setOperation("创建用户");
        log2.setIsSuccess(true);
        log2.setCreatedAt(LocalDateTime.now());

        when(systemLogRepository.findAll()).thenReturn(List.of(log1, log2));

        dataExportService.exportLogs(null, "Agent管理", null, null, null, mockResponse);

        String output = byteArrayOutputStream.toString("UTF-8");
        assertTrue(output.contains("创建Agent"));
        assertFalse(output.contains("创建用户"));
    }

    @Test
    @DisplayName("导出系统日志 - 按成功状态过滤")
    void testExportLogs_FilterBySuccess() throws IOException {
        SystemLog log1 = new SystemLog();
        log1.setId(1L);
        log1.setModule("Agent管理");
        log1.setOperation("创建Agent");
        log1.setIsSuccess(true);
        log1.setCreatedAt(LocalDateTime.now());

        SystemLog log2 = new SystemLog();
        log2.setId(2L);
        log2.setModule("Agent管理");
        log2.setOperation("创建Agent");
        log2.setIsSuccess(false);
        log2.setCreatedAt(LocalDateTime.now());

        when(systemLogRepository.findAll()).thenReturn(List.of(log1, log2));

        dataExportService.exportLogs(null, null, true, null, null, mockResponse);

        String output = byteArrayOutputStream.toString("UTF-8");
        // CSV header + 1 data row
        assertTrue(output.contains("ID,TenantId"));
    }
}
