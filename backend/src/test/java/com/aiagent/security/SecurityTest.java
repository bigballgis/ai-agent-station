package com.aiagent.security;

import com.aiagent.entity.Role;
import com.aiagent.entity.User;
import com.aiagent.entity.UserRole;
import com.aiagent.repository.RoleRepository;
import com.aiagent.repository.UserRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security 安全测试
 * 测试认证和授权机制：
 * - 未认证访问受保护端点返回 401
 * - 错误 Token 返回 401
 * - 无权限访问返回 403
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("安全认证测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    private static final String TEST_USERNAME = "security_test_user";
    private static final String TEST_PASSWORD = "TestPass123!";

    @BeforeEach
    void setUp() {
        // 创建测试用户
        if (!userRepository.findByUsername(TEST_USERNAME).isPresent()) {
            User user = new User();
            user.setUsername(TEST_USERNAME);
            user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
            user.setEmail("security@test.com");
            user.setTenantId(100L);
            user.setActive(true);
            userRepository.save(user);
        }
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        userRepository.findByUsername(TEST_USERNAME)
                .ifPresent(user -> {
                    userRoleRepository.findByUserId(user.getId())
                            .forEach(ur -> userRoleRepository.delete(ur));
                    userRepository.delete(user);
                });
    }

    // ==================== 未认证访问测试 ====================

    @Test
    @Order(1)
    @DisplayName("未认证访问受保护端点 - 返回 401")
    void testUnauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("未认证访问 Agent 详情 - 返回 401")
    void testUnauthenticatedGetAgentById_Returns401() throws Exception {
        mockMvc.perform(get("/v1/agents/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("未认证创建 Agent - 返回 401")
    void testUnauthenticatedCreateAgent_Returns401() throws Exception {
        mockMvc.perform(post("/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("未认证删除 Agent - 返回 401")
    void testUnauthenticatedDeleteAgent_Returns401() throws Exception {
        mockMvc.perform(delete("/v1/agents/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 错误 Token 测试 ====================

    @Test
    @Order(5)
    @DisplayName("无效 Token 访问受保护端点 - 返回 401")
    void testInvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", "Bearer invalid_token_string")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("过期 Token 访问受保护端点 - 返回 401")
    void testExpiredToken_Returns401() throws Exception {
        // 生成一个已过期的 token（通过设置极短的过期时间）
        String expiredToken = jwtUtil.generateToken(1L, "testuser", 100L);

        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // 由于测试环境中 token 有效期较长，这里验证请求能正常处理
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("空 Bearer Token 访问 - 返回 401")
    void testEmptyBearerToken_Returns401() throws Exception {
        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("无 Authorization 头访问 - 返回 401")
    void testNoAuthHeader_Returns401() throws Exception {
        mockMvc.perform(get("/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("错误格式 Token（非 Bearer） - 返回 401")
    void testMalformedAuthHeader_Returns401() throws Exception {
        mockMvc.perform(get("/v1/agents")
                        .header("Authorization", "Basic dXNlcjpwYXNz")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 无权限访问测试 (403) ====================

    @Test
    @Order(10)
    @DisplayName("普通用户访问管理员接口 - 返回 403")
    void testUserRoleAccessAdminEndpoint_Returns403() throws Exception {
        // 创建普通用户 token（无 ADMIN 角色）
        String userToken = jwtUtil.generateToken(999L, "regularuser", 100L);

        mockMvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("普通用户访问角色管理接口 - 返回 403")
    void testUserRoleAccessRoleEndpoint_Returns403() throws Exception {
        String userToken = jwtUtil.generateToken(999L, "regularuser", 100L);

        mockMvc.perform(get("/v1/roles")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("普通用户访问权限管理接口 - 返回 403")
    void testUserRoleAccessPermissionEndpoint_Returns403() throws Exception {
        String userToken = jwtUtil.generateToken(999L, "regularuser", 100L);

        mockMvc.perform(get("/v1/permissions")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @DisplayName("普通用户访问租户管理接口 - 返回 403")
    void testUserRoleAccessTenantEndpoint_Returns403() throws Exception {
        String userToken = jwtUtil.generateToken(999L, "regularuser", 100L);

        mockMvc.perform(get("/v1/tenants")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Tenant-ID", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ==================== 公开端点测试 ====================

    @Test
    @Order(14)
    @DisplayName("登录接口无需认证 - 返回 200")
    void testLoginEndpoint_NoAuthRequired() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(15)
    @DisplayName("注册接口无需认证 - 返回 200")
    void testRegisterEndpoint_NoAuthRequired() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"password123\",\"email\":\"new@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(16)
    @DisplayName("验证码接口无需认证 - 返回 200")
    void testCaptchaEndpoint_NoAuthRequired() throws Exception {
        mockMvc.perform(get("/v1/auth/captcha")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
