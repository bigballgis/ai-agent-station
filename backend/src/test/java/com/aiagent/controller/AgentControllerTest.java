package com.aiagent.controller;

import com.aiagent.entity.Agent;
import com.aiagent.service.AgentService;
import com.aiagent.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentController 单元测试
 * 使用 MockMvc 测试 Agent 管理接口
 */
@WebMvcTest(AgentController.class)
@DisplayName("Agent控制器测试")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentService agentService;

    private Agent testAgent;

    @BeforeEach
    void setUp() {
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setTenantId(100L);
        testAgent.setName("测试Agent");
        testAgent.setDescription("测试描述");
        testAgent.setConfig(new HashMap<>());
        testAgent.setIsActive(true);
    }

    @Test
    @DisplayName("获取所有Agent - 成功返回200")
    void testGetAllAgents() throws Exception {
        Agent agent2 = new Agent();
        agent2.setId(2L);
        agent2.setName("Agent2");

        when(agentService.getAllAgents()).thenReturn(List.of(testAgent, agent2));

        mockMvc.perform(get("/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("测试Agent"))
                .andExpect(jsonPath("$.data[1].name").value("Agent2"));
    }

    @Test
    @DisplayName("根据ID获取Agent - 成功返回200")
    void testGetAgentById() throws Exception {
        when(agentService.getAgentById(1L)).thenReturn(testAgent);

        mockMvc.perform(get("/agents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("测试Agent"));
    }

    @Test
    @DisplayName("创建Agent - 成功返回200")
    void testCreateAgent() throws Exception {
        Agent savedAgent = new Agent();
        savedAgent.setId(3L);
        savedAgent.setName("新Agent");
        savedAgent.setDescription("新描述");
        savedAgent.setIsActive(true);

        when(agentService.createAgent(any(Agent.class))).thenReturn(savedAgent);

        String json = objectMapper.writeValueAsString(Map.of(
                "name", "新Agent",
                "description", "新描述"
        ));

        mockMvc.perform(post("/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("新Agent"));
    }

    @Test
    @DisplayName("删除Agent - 成功返回200")
    void testDeleteAgent() throws Exception {
        doNothing().when(agentService).deleteAgent(1L);

        mockMvc.perform(delete("/agents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(agentService).deleteAgent(1L);
    }

    @Test
    @DisplayName("更新Agent - 成功返回200")
    void testUpdateAgent() throws Exception {
        Agent updatedAgent = new Agent();
        updatedAgent.setId(1L);
        updatedAgent.setName("更新后");
        updatedAgent.setDescription("更新后描述");

        when(agentService.updateAgent(eq(1L), any(Agent.class))).thenReturn(updatedAgent);

        String json = objectMapper.writeValueAsString(Map.of(
                "name", "更新后",
                "description", "更新后描述"
        ));

        mockMvc.perform(put("/agents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("更新后"));
    }

    @Test
    @DisplayName("复制Agent - 成功返回200")
    void testCopyAgent() throws Exception {
        Agent copiedAgent = new Agent();
        copiedAgent.setId(4L);
        copiedAgent.setName("复制的Agent");

        when(agentService.copyAgent(eq(1L), eq("复制的Agent"))).thenReturn(copiedAgent);

        String json = objectMapper.writeValueAsString(Map.of(
                "newName", "复制的Agent"
        ));

        mockMvc.perform(post("/agents/1/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("复制的Agent"));
    }

    @Test
    @DisplayName("复制Agent - 名称为空返回400")
    void testCopyAgent_EmptyName() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of());

        mockMvc.perform(post("/agents/1/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
