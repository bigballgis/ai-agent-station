package com.aiagent.integration;

import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.repository.WorkflowDefinitionRepository;
import com.aiagent.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
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
 * WorkflowController 集成测试
 * 使用 @SpringBootTest + @AutoConfigureMockMvc 进行端到端 API 测试
 * 覆盖工作流定义的 CRUD、发布、新版本创建、回滚等操作
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("工作流控制器集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Integration test requires full Spring environment (DB/Redis). Enable in dedicated integration test pipeline.")
class WorkflowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String authToken;
    private Long testDefinitionId;

    @BeforeEach
    void setUp() {
        workflowDefinitionRepository.deleteAll();

        // 生成测试用 JWT Token
        authToken = "Bearer " + jwtUtil.generateToken(1L, "testadmin", 100L);
    }

    @AfterEach
    void tearDown() {
        workflowDefinitionRepository.deleteAll();
    }

    // ==================== 创建工作流定义测试 ====================

    @Test
    @Order(1)
    @DisplayName("创建工作流定义 - 有效数据返回200")
    void testCreateDefinition_WithValidData_Returns200() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "集成测试工作流");
        requestBody.put("description", "集成测试工作流描述");
        requestBody.put("nodes", Map.of("node1", Map.of("type", "start", "label", "开始")));
        requestBody.put("edges", Map.of("edge1", Map.of("from", "node1", "to", "node2")));

        mockMvc.perform(post("/v1/workflows/definitions")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.name").value("集成测试工作流"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // 验证数据库中已保存
        WorkflowDefinition saved = workflowDefinitionRepository.findAll().stream()
                .filter(d -> "集成测试工作流".equals(d.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(saved, "工作流定义应已创建");
        testDefinitionId = saved.getId();
        assertEquals(1, saved.getVersion());
        assertEquals(WorkflowDefinition.WorkflowStatus.DRAFT, saved.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("创建工作流定义 - 名称为空返回错误")
    void testCreateDefinition_WithEmptyName_ReturnsError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "");
        requestBody.put("description", "测试描述");

        mockMvc.perform(post("/v1/workflows/definitions")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("创建工作流定义 - 无名称字段返回400")
    void testCreateDefinition_WithoutName_Returns400() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("description", "只有描述没有名称");

        mockMvc.perform(post("/v1/workflows/definitions")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 查询工作流定义列表测试 ====================

    @Test
    @Order(4)
    @DisplayName("分页查询工作流定义列表 - 返回分页数据")
    void testListDefinitions_ReturnsPaginatedList() throws Exception {
        // 创建多个测试数据
        for (int i = 1; i <= 5; i++) {
            WorkflowDefinition def = new WorkflowDefinition();
            def.setTenantId(100L);
            def.setName("工作流定义-" + i);
            def.setDescription("测试工作流描述-" + i);
            def.setVersion(1);
            def.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
            def.setBaseDefinitionId(0L); // will be set after save
            workflowDefinitionRepository.save(def);
        }

        mockMvc.perform(get("/v1/workflows/definitions")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(5)));
    }

    @Test
    @Order(5)
    @DisplayName("分页查询工作流定义列表 - 按状态筛选")
    void testListDefinitions_FilterByStatus() throws Exception {
        // 创建不同状态的工作流定义
        WorkflowDefinition draftDef = new WorkflowDefinition();
        draftDef.setTenantId(100L);
        draftDef.setName("草稿工作流");
        draftDef.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        draftDef.setVersion(1);
        workflowDefinitionRepository.save(draftDef);

        WorkflowDefinition publishedDef = new WorkflowDefinition();
        publishedDef.setTenantId(100L);
        publishedDef.setName("已发布工作流");
        publishedDef.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        publishedDef.setVersion(1);
        workflowDefinitionRepository.save(publishedDef);

        mockMvc.perform(get("/v1/workflows/definitions")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))));
    }

    // ==================== 获取工作流定义详情测试 ====================

    @Test
    @Order(6)
    @DisplayName("根据ID获取工作流定义详情 - 返回正确数据")
    void testGetDefinitionById_ReturnsDefinition() throws Exception {
        // 创建测试数据
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("详情测试工作流");
        def.setDescription("详情测试描述");
        def.setVersion(2);
        def.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        mockMvc.perform(get("/v1/workflows/definitions/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.name").value("详情测试工作流"))
                .andExpect(jsonPath("$.data.description").value("详情测试描述"))
                .andExpect(jsonPath("$.data.version").value(2));
    }

    @Test
    @Order(7)
    @DisplayName("根据ID获取工作流定义详情 - 不存在返回错误")
    void testGetDefinitionById_NotFound_ReturnsError() throws Exception {
        mockMvc.perform(get("/v1/workflows/definitions/99999")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 更新工作流定义测试 ====================

    @Test
    @Order(8)
    @DisplayName("更新工作流定义 - 草稿状态可以更新")
    void testUpdateDefinition_DraftStatus_ReturnsUpdatedData() throws Exception {
        // 创建测试数据
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("更新前工作流");
        def.setDescription("更新前描述");
        def.setVersion(1);
        def.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "更新后工作流");
        updateBody.put("description", "更新后描述");

        mockMvc.perform(put("/v1/workflows/definitions/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("更新后工作流"))
                .andExpect(jsonPath("$.data.description").value("更新后描述"));
    }

    @Test
    @Order(9)
    @DisplayName("更新工作流定义 - 已发布状态不能直接修改")
    void testUpdateDefinition_PublishedStatus_ReturnsError() throws Exception {
        // 创建已发布的工作流定义
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("已发布工作流");
        def.setDescription("已发布描述");
        def.setVersion(1);
        def.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "尝试修改已发布工作流");

        mockMvc.perform(put("/v1/workflows/definitions/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 删除工作流定义测试 ====================

    @Test
    @Order(10)
    @DisplayName("删除工作流定义 - 草稿状态可以删除")
    void testDeleteDefinition_DraftStatus_Returns200() throws Exception {
        // 创建测试数据
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("待删除工作流");
        def.setDescription("待删除描述");
        def.setVersion(1);
        def.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        mockMvc.perform(delete("/v1/workflows/definitions/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证数据库中已删除
        assertFalse(workflowDefinitionRepository.findById(saved.getId()).isPresent(),
                "工作流定义应已删除");
    }

    @Test
    @Order(11)
    @DisplayName("删除工作流定义 - 已发布状态不能删除")
    void testDeleteDefinition_PublishedStatus_ReturnsError() throws Exception {
        // 创建已发布的工作流定义
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("已发布不可删除工作流");
        def.setDescription("已发布描述");
        def.setVersion(1);
        def.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        mockMvc.perform(delete("/v1/workflows/definitions/" + saved.getId())
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));

        // 验证数据库中未被删除
        assertTrue(workflowDefinitionRepository.findById(saved.getId()).isPresent(),
                "已发布的工作流定义不应被删除");
    }

    // ==================== 发布工作流定义测试 ====================

    @Test
    @Order(12)
    @DisplayName("发布工作流定义 - 草稿状态可以发布")
    void testPublishDefinition_DraftStatus_ReturnsPublished() throws Exception {
        // 创建草稿工作流定义
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("待发布工作流");
        def.setDescription("待发布描述");
        def.setVersion(1);
        def.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        mockMvc.perform(post("/v1/workflows/definitions/" + saved.getId() + "/publish")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @Order(13)
    @DisplayName("发布工作流定义 - 已发布状态重复发布返回错误")
    void testPublishDefinition_AlreadyPublished_ReturnsError() throws Exception {
        // 创建已发布的工作流定义
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("已发布工作流");
        def.setDescription("已发布描述");
        def.setVersion(1);
        def.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        mockMvc.perform(post("/v1/workflows/definitions/" + saved.getId() + "/publish")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 创建新版本测试 ====================

    @Test
    @Order(14)
    @DisplayName("创建新版本 - 基于已发布定义创建新草稿版本")
    void testCreateNewVersion_FromPublishedDefinition_ReturnsNewVersion() throws Exception {
        // 创建已发布的工作流定义
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("新版本测试工作流");
        def.setDescription("新版本测试描述");
        def.setVersion(1);
        def.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        mockMvc.perform(post("/v1/workflows/definitions/" + saved.getId() + "/new-version")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.version").value(greaterThan(1)));
    }

    // ==================== 回滚版本测试 ====================

    @Test
    @Order(15)
    @DisplayName("回滚到指定版本 - 创建新草稿")
    void testRollbackToVersion_ReturnsNewDraft() throws Exception {
        // 创建工作流定义
        WorkflowDefinition def = new WorkflowDefinition();
        def.setTenantId(100L);
        def.setName("回滚测试工作流");
        def.setDescription("回滚测试描述");
        def.setVersion(1);
        def.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        def.setBaseDefinitionId(0L);
        WorkflowDefinition saved = workflowDefinitionRepository.save(def);
        saved.setBaseDefinitionId(saved.getId());
        workflowDefinitionRepository.save(saved);

        mockMvc.perform(post("/v1/workflows/definitions/" + saved.getId() + "/rollback/1")
                        .header("Authorization", authToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    // ==================== 分页参数测试 ====================

    @Test
    @Order(16)
    @DisplayName("分页查询 - 自定义分页参数")
    void testListDefinitions_CustomPagination() throws Exception {
        // 创建多个测试数据
        for (int i = 1; i <= 15; i++) {
            WorkflowDefinition def = new WorkflowDefinition();
            def.setTenantId(100L);
            def.setName("分页测试工作流-" + i);
            def.setVersion(1);
            def.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
            workflowDefinitionRepository.save(def);
        }

        // 请求第一页，每页5条
        mockMvc.perform(get("/v1/workflows/definitions")
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
