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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentController 集成测试
 * 使用 @SpringBootTest + @AutoConfigureMockMvc 进行端到端 API 测试
 * 覆盖 Agent 的 CRUD、复制、模板查询、导入等操作
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

    @AfterEach
    void tearDown() {
        agentRepository.deleteAll();
    }

    // ==================== 创建 Agent 测试 ====================

    @Test
    @Order(1)
    @DisplayName("创建Agent - 有效数据返回200")
    void testCreateAgent_WithValidData_Returns200() throws Exception {
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
        assertNotNull(saved, "Agent应已创建");
        testAgentId = saved.getId();
    }

    @Test
    @Order(2)
    @DisplayName("创建Agent - 名称为空返回400")
    void testCreateAgent_WithEmptyName_Returns400() throws Exception {
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

    @Test
    @Order(3)
    @DisplayName("创建Agent - 带类型和分类返回200")
    void testCreateAgent_WithTypeAndCategory_Returns200() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "分类测试Agent");
        requestBody.put("description", "带分类的Agent");
        requestBody.put("type", "CHAT");
        requestBody.put("category", "customer-service");
        requestBody.put("isActive", true);

        mockMvc.perform(post("/v1/agents")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("分类测试Agent"));
    }

    // ==================== 查询 Agent 列表测试 ====================

    @Test
    @Order(4)
    @DisplayName("获取Agent列表 - 返回分页数据")
    void testGetAllAgents_ReturnsPaginatedList() throws Exception {
        // 创建多个测试数据
        for (int i = 1; i <= 5; i++) {
            Agent agent = new Agent();
            agent.setTenantId(100L);
            agent.setName("列表测试Agent-" + i);
            agent.setDescription("列表测试描述-" + i);
            agent.setIsActive(true);
            agentRepository.save(agent);
        }

        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(5)));
    }

    @Test
    @Order(5)
    @DisplayName("获取Agent列表 - 按关键词搜索")
    void testGetAllAgents_WithKeywordFilter() throws Exception {
        Agent agent1 = new Agent();
        agent1.setTenantId(100L);
        agent1.setName("搜索目标Agent");
        agent1.setDescription("可被搜索到的Agent");
        agent1.setIsActive(true);
        agentRepository.save(agent1);

        Agent agent2 = new Agent();
        agent2.setTenantId(100L);
        agent2.setName("其他Agent");
        agent2.setDescription("不相关的描述");
        agent2.setIsActive(true);
        agentRepository.save(agent2);

        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "20")
                        .param("keyword", "搜索目标"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(6)
    @DisplayName("获取Agent列表 - 按状态筛选")
    void testGetAllAgents_WithStatusFilter() throws Exception {
        Agent draftAgent = new Agent();
        draftAgent.setTenantId(100L);
        draftAgent.setName("草稿Agent");
        draftAgent.setStatus(Agent.AgentStatus.DRAFT);
        draftAgent.setIsActive(true);
        agentRepository.save(draftAgent);

        Agent publishedAgent = new Agent();
        publishedAgent.setTenantId(100L);
        publishedAgent.setName("已发布Agent");
        publishedAgent.setStatus(Agent.AgentStatus.PUBLISHED);
        publishedAgent.setIsActive(true);
        agentRepository.save(publishedAgent);

        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))));
    }

    // ==================== 获取 Agent 详情测试 ====================

    @Test
    @Order(7)
    @DisplayName("根据ID获取Agent - 返回正确数据")
    void testGetAgentById_ReturnsAgent() throws Exception {
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("详情测试Agent");
        agent.setDescription("详情测试描述");
        agent.setIsActive(true);
        agent.setCategory("data-analysis");
        Agent saved = agentRepository.save(agent);

        mockMvc.perform(get("/v1/agents/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.name").value("详情测试Agent"))
                .andExpect(jsonPath("$.data.description").value("详情测试描述"));
    }

    @Test
    @Order(8)
    @DisplayName("根据ID获取Agent - 不存在返回错误")
    void testGetAgentById_NotFound_ReturnsError() throws Exception {
        mockMvc.perform(get("/v1/agents/99999")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 更新 Agent 测试 ====================

    @Test
    @Order(9)
    @DisplayName("更新Agent - 返回更新后数据")
    void testUpdateAgent_ReturnsUpdatedData() throws Exception {
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("更新前Agent");
        agent.setDescription("更新前描述");
        agent.setIsActive(true);
        Agent saved = agentRepository.save(agent);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "更新后Agent");
        updateBody.put("description", "更新后描述");
        updateBody.put("isActive", false);

        mockMvc.perform(put("/v1/agents/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("更新后Agent"))
                .andExpect(jsonPath("$.data.description").value("更新后描述"));
    }

    @Test
    @Order(10)
    @DisplayName("更新Agent - 更新类型和分类")
    void testUpdateAgent_UpdateTypeAndCategory() throws Exception {
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("类型更新Agent");
        agent.setStatus(Agent.AgentStatus.DRAFT);
        agent.setIsActive(true);
        Agent saved = agentRepository.save(agent);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "类型更新Agent");
        updateBody.put("type", "TASK");
        updateBody.put("category", "code-generation");

        mockMvc.perform(put("/v1/agents/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 删除 Agent 测试 ====================

    @Test
    @Order(11)
    @DisplayName("删除Agent - 返回200并从数据库移除")
    void testDeleteAgent_Returns200AndRemovesFromDb() throws Exception {
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("待删除Agent");
        agent.setDescription("待删除描述");
        agent.setIsActive(true);
        Agent saved = agentRepository.save(agent);

        mockMvc.perform(delete("/v1/agents/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证数据库中已删除
        assertFalse(agentRepository.findById(saved.getId()).isPresent(),
                "Agent应已删除");
    }

    @Test
    @Order(12)
    @DisplayName("删除Agent - 不存在的ID返回错误")
    void testDeleteAgent_NotFound_ReturnsError() throws Exception {
        mockMvc.perform(delete("/v1/agents/99999")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 复制 Agent 测试 ====================

    @Test
    @Order(13)
    @DisplayName("复制Agent - 返回新Agent数据")
    void testCopyAgent_ReturnsNewAgent() throws Exception {
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("原始Agent");
        agent.setDescription("原始描述");
        agent.setIsActive(true);
        agent.setCategory("general");
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

        // 验证原始Agent仍然存在
        assertTrue(agentRepository.findById(saved.getId()).isPresent(),
                "原始Agent应仍然存在");
    }

    @Test
    @Order(14)
    @DisplayName("复制Agent - 新名称为空返回400")
    void testCopyAgent_WithEmptyName_Returns400() throws Exception {
        Agent agent = new Agent();
        agent.setTenantId(100L);
        agent.setName("复制测试Agent");
        agent.setIsActive(true);
        Agent saved = agentRepository.save(agent);

        Map<String, String> copyBody = new HashMap<>();
        copyBody.put("newName", "");

        mockMvc.perform(post("/v1/agents/" + saved.getId() + "/copy")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copyBody)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 模板列表测试 ====================

    @Test
    @Order(15)
    @DisplayName("获取模板列表 - 返回分页数据")
    void testGetTemplates_ReturnsPaginatedList() throws Exception {
        // 创建模板Agent
        Agent template1 = new Agent();
        template1.setTenantId(100L);
        template1.setName("客服模板");
        template1.setDescription("客服助手模板");
        template1.setIsTemplate(true);
        template1.setIsActive(true);
        template1.setUsageCount(100);
        agentRepository.save(template1);

        Agent template2 = new Agent();
        template2.setTenantId(100L);
        template2.setName("数据分析模板");
        template2.setDescription("数据分析模板");
        template2.setIsTemplate(true);
        template2.setIsActive(true);
        template2.setUsageCount(50);
        agentRepository.save(template2);

        mockMvc.perform(get("/v1/agents/templates")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @Order(16)
    @DisplayName("获取模板列表 - 按分类筛选")
    void testGetTemplates_FilterByCategory() throws Exception {
        Agent template = new Agent();
        template.setTenantId(100L);
        template.setName("分类模板");
        template.setCategory("customer-service");
        template.setIsTemplate(true);
        template.setIsActive(true);
        agentRepository.save(template);

        mockMvc.perform(get("/v1/agents/templates")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "20")
                        .param("category", "customer-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== 导入 Agent 测试 ====================

    @Test
    @Order(17)
    @DisplayName("导入Agent - 有效数据返回200")
    void testImportAgent_WithValidData_Returns200() throws Exception {
        Map<String, Object> importData = new HashMap<>();
        importData.put("name", "导入测试Agent");
        importData.put("description", "从JSON导入的Agent");
        importData.put("category", "general");
        importData.put("isActive", true);
        importData.put("status", "DRAFT");

        mockMvc.perform(post("/v1/agents/import")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.name").value("导入测试Agent"));

        // 验证数据库中已创建
        Agent imported = agentRepository.findAll().stream()
                .filter(a -> "导入测试Agent".equals(a.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(imported, "导入的Agent应已创建");
    }

    @Test
    @Order(18)
    @DisplayName("导入Agent - 名称冲突自动添加后缀")
    void testImportAgent_WithDuplicateName_AppendsSuffix() throws Exception {
        // 先创建同名Agent
        Agent existing = new Agent();
        existing.setTenantId(100L);
        existing.setName("重名Agent");
        existing.setIsActive(true);
        agentRepository.save(existing);

        Map<String, Object> importData = new HashMap<>();
        importData.put("name", "重名Agent");
        importData.put("description", "名称冲突的导入");

        mockMvc.perform(post("/v1/agents/import")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value(not("重名Agent")));
    }

    @Test
    @Order(19)
    @DisplayName("导入Agent - 缺少名称返回错误")
    void testImportAgent_WithoutName_ReturnsError() throws Exception {
        Map<String, Object> importData = new HashMap<>();
        importData.put("description", "没有名称的导入");

        mockMvc.perform(post("/v1/agents/import")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 分页参数测试 ====================

    @Test
    @Order(20)
    @DisplayName("获取Agent列表 - 自定义分页参数")
    void testGetAllAgents_CustomPagination() throws Exception {
        // 创建多个测试数据
        for (int i = 1; i <= 15; i++) {
            Agent agent = new Agent();
            agent.setTenantId(100L);
            agent.setName("分页Agent-" + i);
            agent.setIsActive(true);
            agentRepository.save(agent);
        }

        // 请求第一页，每页5条
        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.records", hasSize(5)))
                .andExpect(jsonPath("$.data.size").value(5));
    }
}
