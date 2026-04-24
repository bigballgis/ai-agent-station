package com.aiagent.integration;

import com.aiagent.entity.Agent;
import com.aiagent.repository.AgentRepository;
import com.aiagent.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentController 集成测试
 * 使用 @SpringBootTest + @AutoConfigureMockMvc 进行端到端 API 测试
 * 使用 H2 内存数据库，无需外部依赖
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Agent控制器集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String authToken;
    private Long testAgentId;

    @BeforeEach
    void setUp() {
        agentRepository.deleteAll();

        // 生成测试用 JWT Token
        authToken = "Bearer " + jwtUtil.generateToken(1L, "testadmin", 100L);
    }

    @Test
    @Order(1)
    @DisplayName("创建Agent - 集成测试")
    void testCreateAgent() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "集成测试Agent");
        requestBody.put("description", "集成测试描述");
        requestBody.put("isActive", true);

        mockMvc.perform(post("/v1/agents")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("集成测试Agent"))
                .andExpect(jsonPath("$.data.description").value("集成测试描述"));

        // 验证数据库中已保存
        Agent saved = agentRepository.findAll().stream()
                .filter(a -> "集成测试Agent".equals(a.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(saved);
        testAgentId = saved.getId();
    }

    @Test
    @Order(2)
    @DisplayName("获取Agent列表 - 集成测试")
    void testGetAllAgents() throws Exception {
        // 先创建测试数据
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("列表测试Agent");
        agent.setDescription("列表测试");
        agent.setIsActive(true);
        agentRepository.save(agent);

        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @Order(3)
    @DisplayName("根据ID获取Agent - 集成测试")
    void testGetAgentById() throws Exception {
        // 先创建测试数据
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("详情测试Agent");
        agent.setDescription("详情测试");
        agent.setIsActive(true);
        Agent saved = agentRepository.save(agent);

        mockMvc.perform(get("/v1/agents/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.name").value("详情测试Agent"));
    }

    @Test
    @Order(4)
    @DisplayName("更新Agent - 集成测试")
    void testUpdateAgent() throws Exception {
        // 先创建测试数据
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("更新前Agent");
        agent.setDescription("更新前描述");
        agent.setIsActive(true);
        Agent saved = agentRepository.save(agent);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "更新后Agent");
        updateBody.put("description", "更新后描述");

        mockMvc.perform(put("/v1/agents/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("更新后Agent"));
    }

    @Test
    @Order(5)
    @DisplayName("删除Agent - 集成测试")
    void testDeleteAgent() throws Exception {
        // 先创建测试数据
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("待删除Agent");
        agent.setDescription("待删除");
        agent.setIsActive(true);
        Agent saved = agentRepository.save(agent);

        mockMvc.perform(delete("/v1/agents/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证数据库中已删除
        Assertions.assertFalse(agentRepository.findById(saved.getId()).isPresent());
    }

    @Test
    @Order(6)
    @DisplayName("复制Agent - 集成测试")
    void testCopyAgent() throws Exception {
        // 先创建测试数据
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("原始Agent");
        agent.setDescription("原始描述");
        agent.setIsActive(true);
        Agent saved = agentRepository.save(agent);

        Map<String, String> copyBody = new HashMap<>();
        copyBody.put("newName", "复制的Agent");

        mockMvc.perform(post("/v1/agents/" + saved.getId() + "/copy")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("复制的Agent"));
    }

    @Test
    @Order(7)
    @DisplayName("获取不存在的Agent - 返回错误")
    void testGetNonExistentAgent() throws Exception {
        mockMvc.perform(get("/v1/agents/99999")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(8)
    @DisplayName("创建Agent - 名称为空返回错误")
    void testCreateAgentWithEmptyName() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "");
        requestBody.put("description", "测试");

        mockMvc.perform(post("/v1/agents")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }
}
