package com.aiagent.integration;

import com.aiagent.entity.Tenant;
import com.aiagent.repository.TenantRepository;
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
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TenantController 集成测试
 * 使用 @SpringBootTest + MockMvc 进行端到端 API 测试
 * 覆盖租户的增删改查及权限控制
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("租户控制器集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Integration test requires full Spring environment (DB/Redis). Enable in dedicated integration test pipeline.")
class TenantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantRepository tenantRepository;

    private static final String TEST_TENANT_NAME = "集成测试租户";

    private String adminToken;
    private Long testTenantId;

    @BeforeEach
    void setUp() {
        // 清理测试租户
        tenantRepository.findByName(TEST_TENANT_NAME).ifPresent(tenant -> {
            tenantRepository.delete(tenant);
        });

        // 生成管理员 Token（需要 ADMIN 角色才能访问租户接口）
        adminToken = "Bearer " + jwtUtil.generateToken(1L, "admin", null);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        tenantRepository.findByName(TEST_TENANT_NAME).ifPresent(tenant -> {
            tenantRepository.delete(tenant);
        });
    }

    // ==================== 权限控制测试 ====================

    @Test
    @Order(1)
    @DisplayName("获取租户列表 - 无管理员角色返回403")
    void testGetAllTenants_WithoutAdminRole_Returns403() throws Exception {
        // 生成普通用户 Token（无 ADMIN 角色）
        String userToken = "Bearer " + jwtUtil.generateToken(999L, "regularuser", 100L);

        mockMvc.perform(get("/v1/tenants")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    @DisplayName("获取租户列表 - 未认证返回401")
    void testGetAllTenants_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 创建租户测试 ====================

    @Test
    @Order(3)
    @DisplayName("创建租户 - 有效数据返回201")
    void testCreateTenant_WithValidData_Returns201() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", TEST_TENANT_NAME);
        requestBody.put("description", "集成测试租户描述");
        requestBody.put("maxAgents", 50);
        requestBody.put("maxStorageMb", 512L);

        mockMvc.perform(post("/v1/tenants")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.name").value(TEST_TENANT_NAME));

        // 验证数据库中已保存
        Optional<Tenant> created = tenantRepository.findByName(TEST_TENANT_NAME);
        assertTrue(created.isPresent(), "租户应已创建");
        testTenantId = created.get().getId();
    }

    @Test
    @Order(4)
    @DisplayName("创建租户 - 缺少名称返回400")
    void testCreateTenant_WithMissingName_Returns400() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("description", "缺少名称的租户");

        mockMvc.perform(post("/v1/tenants")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("创建租户 - 重复名称返回错误")
    void testCreateTenant_WithDuplicateName_ReturnsError() throws Exception {
        // 先创建租户
        Tenant existing = new Tenant();
        existing.setName(TEST_TENANT_NAME);
        existing.setIsActive(true);
        tenantRepository.save(existing);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", TEST_TENANT_NAME);
        requestBody.put("description", "重复名称租户");

        mockMvc.perform(post("/v1/tenants")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 获取租户列表测试 ====================

    @Test
    @Order(6)
    @DisplayName("获取租户列表 - 管理员角色返回列表")
    void testGetAllTenants_WithAdminRole_ReturnsList() throws Exception {
        // 先创建测试数据
        Tenant tenant = new Tenant();
        tenant.setName("列表测试租户_" + System.currentTimeMillis());
        tenant.setDescription("列表测试");
        tenant.setIsActive(true);
        tenantRepository.save(tenant);

        mockMvc.perform(get("/v1/tenants")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== 更新租户测试 ====================

    @Test
    @Order(7)
    @DisplayName("更新租户 - 成功更新")
    void testUpdateTenant_Success() throws Exception {
        // 先创建租户
        Tenant tenant = new Tenant();
        tenant.setName("更新前租户_" + System.currentTimeMillis());
        tenant.setDescription("更新前描述");
        tenant.setIsActive(true);
        Tenant saved = tenantRepository.save(tenant);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "更新后租户");
        updateBody.put("description", "更新后描述");

        mockMvc.perform(put("/v1/tenants/" + saved.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== 删除租户测试（软删除） ====================

    @Test
    @Order(8)
    @DisplayName("删除租户 - 软删除成功")
    void testDeleteTenant_SoftDelete() throws Exception {
        // 先创建租户
        Tenant tenant = new Tenant();
        tenant.setName("待删除租户_" + System.currentTimeMillis());
        tenant.setDescription("待删除");
        tenant.setIsActive(true);
        Tenant saved = tenantRepository.save(tenant);

        mockMvc.perform(delete("/v1/tenants/" + saved.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证租户已被软删除（isActive = false）
        Tenant deleted = tenantRepository.findById(saved.getId()).orElse(null);
        assertNotNull(deleted, "租户记录应仍存在（软删除）");
        assertFalse(deleted.getIsActive(), "租户应已被停用");
    }

    @Test
    @Order(9)
    @DisplayName("删除租户 - 不存在的租户返回错误")
    void testDeleteTenant_NotFound_ReturnsError() throws Exception {
        mockMvc.perform(delete("/v1/tenants/99999")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }
}
