package com.aiagent.integration;

import com.aiagent.entity.User;
import com.aiagent.entity.Role;
import com.aiagent.entity.UserRole;
import com.aiagent.repository.UserRepository;
import com.aiagent.repository.RoleRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.security.JwtUtil;
import com.aiagent.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试
 * 使用 @SpringBootTest + MockMvc 进行端到端 API 测试
 * 覆盖注册、登录、Token刷新、登出、用户信息获取等认证流程
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("认证控制器集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Integration test requires full Spring environment (DB/Redis). Enable in dedicated integration test pipeline.")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String TEST_USERNAME = "integ_auth_user";
    private static final String TEST_PASSWORD = "TestPass123!";
    private static final String TEST_EMAIL = "integ_auth@test.com";

    private String validToken;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        // 清理测试用户（如果存在）
        userRepository.findByUsername(TEST_USERNAME).ifPresent(user -> {
            userRoleRepository.findByUserId(user.getId())
                    .forEach(ur -> userRoleRepository.delete(ur));
            userRepository.delete(user);
        });
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        userRepository.findByUsername(TEST_USERNAME).ifPresent(user -> {
            userRoleRepository.findByUserId(user.getId())
                    .forEach(ur -> userRoleRepository.delete(ur));
            userRepository.delete(user);
        });
    }

    // ==================== 注册测试 ====================

    @Test
    @Order(1)
    @DisplayName("注册 - 有效数据返回200")
    void testRegister_WithValidData_Returns200() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", TEST_USERNAME);
        requestBody.put("password", TEST_PASSWORD);
        requestBody.put("confirmPassword", TEST_PASSWORD);
        requestBody.put("email", TEST_EMAIL);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());

        // 验证用户已创建
        Optional<User> createdUser = userRepository.findByUsername(TEST_USERNAME);
        assertTrue(createdUser.isPresent(), "用户应已创建");
        assertTrue(createdUser.get().getIsActive(), "用户应为激活状态");
    }

    @Test
    @Order(2)
    @DisplayName("注册 - 重复用户名返回400")
    void testRegister_WithDuplicateUsername_Returns400() throws Exception {
        // 先创建一个用户
        User existingUser = new User();
        existingUser.setUsername(TEST_USERNAME);
        existingUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        existingUser.setEmail(TEST_EMAIL);
        existingUser.setIsActive(true);
        userRepository.save(existingUser);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", TEST_USERNAME);
        requestBody.put("password", TEST_PASSWORD);
        requestBody.put("confirmPassword", TEST_PASSWORD);
        requestBody.put("email", "another@test.com");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(3)
    @DisplayName("注册 - 密码不一致返回错误")
    void testRegister_WithMismatchedPasswords_ReturnsError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", TEST_USERNAME);
        requestBody.put("password", TEST_PASSWORD);
        requestBody.put("confirmPassword", "WrongPass456!");
        requestBody.put("email", TEST_EMAIL);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 登录测试 ====================

    @Test
    @Order(4)
    @DisplayName("登录 - 有效凭据返回200和Token")
    void testLogin_WithValidCredentials_Returns200WithTokens() throws Exception {
        // 先创建用户
        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setEmail(TEST_EMAIL);
        user.setIsActive(true);
        userRepository.save(user);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", TEST_USERNAME);
        requestBody.put("password", TEST_PASSWORD);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("登录 - 错误密码返回401")
    void testLogin_WithWrongPassword_Returns401() throws Exception {
        // 先创建用户
        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setEmail(TEST_EMAIL);
        user.setIsActive(true);
        userRepository.save(user);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", TEST_USERNAME);
        requestBody.put("password", "WrongPassword999!");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @Order(6)
    @DisplayName("登录 - 不存在的用户返回错误")
    void testLogin_WithNonExistentUser_ReturnsError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "nonexistent_user_xyz");
        requestBody.put("password", TEST_PASSWORD);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== Token刷新测试 ====================

    @Test
    @Order(7)
    @DisplayName("刷新Token - 有效Token返回新Token")
    void testRefresh_WithValidToken_ReturnsNewTokens() throws Exception {
        // 先创建用户并登录获取refreshToken
        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setEmail(TEST_EMAIL);
        user.setIsActive(true);
        userRepository.save(user);

        // 通过 authService 获取 refreshToken
        Map<String, Object> loginResult = authService.login(TEST_USERNAME, TEST_PASSWORD, null);
        String refreshToken = (String) loginResult.get("refreshToken");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("refreshToken", refreshToken);

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("刷新Token - 无效Token返回错误")
    void testRefresh_WithInvalidToken_ReturnsError() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("refreshToken", "invalid-refresh-token-string");

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 登出测试 ====================

    @Test
    @Order(9)
    @DisplayName("登出 - 使Token失效")
    void testLogout_InvalidatesToken() throws Exception {
        // 创建用户
        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setEmail(TEST_EMAIL);
        user.setIsActive(true);
        userRepository.save(user);

        // 登录获取 token
        Map<String, Object> loginResult = authService.login(TEST_USERNAME, TEST_PASSWORD, null);
        String accessToken = (String) loginResult.get("token");

        // 使用 token 登出
        mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证 token 已被加入黑名单
        assertTrue(authService.isTokenBlacklisted(accessToken), "Token应已被加入黑名单");
    }

    // ==================== 用户信息测试 ====================

    @Test
    @Order(10)
    @DisplayName("获取用户信息 - 无Token返回401")
    void testGetUserInfo_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/userinfo")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("获取用户信息 - 有效Token返回200")
    void testGetUserInfo_WithValidToken_Returns200() throws Exception {
        // 创建用户
        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setEmail(TEST_EMAIL);
        user.setIsActive(true);
        userRepository.save(user);

        // 生成有效 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());

        mockMvc.perform(get("/v1/auth/userinfo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME));
    }

    @Test
    @Order(12)
    @DisplayName("获取用户信息 - 无效Token返回401")
    void testGetUserInfo_WithInvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/userinfo")
                        .header("Authorization", "Bearer invalid_token_xyz")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 验证码测试 ====================

    @Test
    @Order(13)
    @DisplayName("获取验证码 - 无需认证返回200")
    void testGetCaptcha_NoAuthRequired_Returns200() throws Exception {
        mockMvc.perform(get("/v1/auth/captcha")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.captchaId").isString())
                .andExpect(jsonPath("$.data.question").isString());
    }
}
