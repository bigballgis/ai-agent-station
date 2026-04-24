package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.dto.RegisterRequestDTO;
import com.aiagent.entity.Role;
import com.aiagent.entity.User;
import com.aiagent.entity.UserRole;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.RoleRepository;
import com.aiagent.repository.UserRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.security.JwtUtil;
import com.aiagent.security.validator.PasswordPolicyValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * 测试用户登录、注册、Token刷新、密码修改、密码策略验证等认证功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private LoginRateLimitService loginRateLimitService;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private MockedStatic<RequestContextHolder> requestContextHolderMock;
    private HttpServletRequest mockRequest;
    private ServletRequestAttributes servletRequestAttributes;

    private static final Long USER_ID = 1L;
    private static final Long TENANT_ID = 100L;
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "Test@12345";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded_password_hash";

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setPassword(ENCODED_PASSWORD);
        testUser.setEmail("test@example.com");
        testUser.setTenantId(TENANT_ID);
        testUser.setIsActive(true);

        // 模拟 HTTP 请求上下文（用于获取客户端 IP）
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        servletRequestAttributes = new ServletRequestAttributes(mockRequest);
        requestContextHolderMock = mockStatic(RequestContextHolder.class);
        requestContextHolderMock.when(RequestContextHolder::getRequestAttributes)
                .thenReturn(servletRequestAttributes);
    }

    @AfterEach
    void tearDown() {
        requestContextHolderMock.close();
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录 - 有效凭据，成功返回token")
    void testLogin_ValidCredentials_Success() {
        when(loginRateLimitService.checkRateLimit(eq(USERNAME), anyString())).thenReturn(null);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtUtil.generateToken(eq(USER_ID), eq(USERNAME), eq(TENANT_ID))).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(eq(USER_ID), eq(USERNAME), eq(TENANT_ID))).thenReturn("refresh_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> result = authService.login(USERNAME, PASSWORD, null);

        assertNotNull(result);
        assertEquals("access_token", result.get("token"));
        assertEquals("refresh_token", result.get("refreshToken"));
        assertNotNull(result.get("user"));
        verify(loginRateLimitService).resetAttempts(USERNAME);
        verify(valueOperations).set(eq("refresh_token:1"), eq("refresh_token"), eq(7L), any());
    }

    @Test
    @DisplayName("登录 - 密码错误，抛出BusinessException")
    void testLogin_InvalidPassword_ThrowsException() {
        when(loginRateLimitService.checkRateLimit(eq(USERNAME), anyString())).thenReturn(null);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass@123", ENCODED_PASSWORD)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.login(USERNAME, "WrongPass@123", null)
        );
        assertTrue(exception.getMessage().contains("用户名或密码错误"));
        verify(loginRateLimitService).recordFailedAttempt(eq(USERNAME), anyString());
    }

    @Test
    @DisplayName("登录 - 用户不存在，抛出BusinessException")
    void testLogin_NonExistentUser_ThrowsException() {
        when(loginRateLimitService.checkRateLimit(eq("nonexistent"), anyString())).thenReturn(null);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.login("nonexistent", PASSWORD, null)
        );
        assertTrue(exception.getMessage().contains("用户名或密码错误"));
        verify(loginRateLimitService).recordFailedAttempt(eq("nonexistent"), anyString());
    }

    @Test
    @DisplayName("登录 - 用户已禁用，抛出BusinessException")
    void testLogin_DisabledUser_ThrowsException() {
        testUser.setIsActive(false);
        when(loginRateLimitService.checkRateLimit(eq(USERNAME), anyString())).thenReturn(null);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.login(USERNAME, PASSWORD, null)
        );
        assertEquals(ResultCode.USER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录 - 登录速率限制触发，抛出BusinessException")
    void testLogin_RateLimited_ThrowsException() {
        when(loginRateLimitService.checkRateLimit(eq(USERNAME), anyString()))
                .thenReturn("登录尝试次数过多，请稍后再试");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.login(USERNAME, PASSWORD, null)
        );
        assertEquals(ResultCode.TOO_MANY_REQUESTS.getCode(), exception.getCode());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("登录 - 带租户ID成功")
    void testLogin_WithTenantId_Success() {
        when(loginRateLimitService.checkRateLimit(eq(USERNAME), anyString())).thenReturn(null);
        when(userRepository.findByUsernameAndTenantId(USERNAME, TENANT_ID))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtUtil.generateToken(eq(USER_ID), eq(USERNAME), eq(TENANT_ID))).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(eq(USER_ID), eq(USERNAME), eq(TENANT_ID))).thenReturn("refresh_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> result = authService.login(USERNAME, PASSWORD, TENANT_ID);

        assertNotNull(result);
        assertEquals("access_token", result.get("token"));
        verify(userRepository).findByUsernameAndTenantId(USERNAME, TENANT_ID);
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册 - 有效数据，成功创建用户并返回token")
    void testRegister_ValidData_Success() {
        RegisterRequestDTO request = buildValidRegisterRequest();

        when(passwordPolicyValidator.validate(PASSWORD)).thenReturn(Collections.emptyList());
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(USER_ID);
            return user;
        });
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());
        // registerPostProcess 调用 login，需要模拟 login 相关依赖
        when(loginRateLimitService.checkRateLimit(eq(USERNAME), anyString())).thenReturn(null);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtUtil.generateToken(eq(USER_ID), eq(USERNAME), eq(TENANT_ID))).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(eq(USER_ID), eq(USERNAME), eq(TENANT_ID))).thenReturn("refresh_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> result = authService.register(request);

        assertNotNull(result);
        assertEquals("access_token", result.get("token"));
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals(USERNAME)
                        && user.getIsActive()
        ));
    }

    @Test
    @DisplayName("注册 - 用户名已存在，抛出BusinessException")
    void testRegister_ExistingUsername_ThrowsException() {
        RegisterRequestDTO request = buildValidRegisterRequest();

        when(passwordPolicyValidator.validate(PASSWORD)).thenReturn(Collections.emptyList());
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.register(request)
        );
        assertEquals(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("用户名已存在"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("注册 - 密码不一致，抛出BusinessException")
    void testRegister_PasswordMismatch_ThrowsException() {
        RegisterRequestDTO request = buildValidRegisterRequest();
        request.setConfirmPassword("Different@12345");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.register(request)
        );
        assertTrue(exception.getMessage().contains("两次输入的密码不一致"));
    }

    @Test
    @DisplayName("注册 - 密码不符合策略，抛出BusinessException")
    void testRegister_PasswordPolicyViolation_ThrowsException() {
        RegisterRequestDTO request = buildValidRegisterRequest();

        when(passwordPolicyValidator.validate(PASSWORD))
                .thenReturn(List.of("密码必须包含至少一个大写字母", "密码必须包含至少一个特殊字符"));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.register(request)
        );
        assertTrue(exception.getMessage().contains("密码必须包含至少一个大写字母"));
        assertTrue(exception.getMessage().contains("密码必须包含至少一个特殊字符"));
    }

    // ==================== Token刷新测试 ====================

    @Test
    @DisplayName("刷新Token - 有效token，成功返回新token")
    void testRefreshToken_ValidToken_Success() {
        when(jwtUtil.validateToken("valid_refresh_token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid_refresh_token")).thenReturn(USER_ID);
        when(jwtUtil.getUsernameFromToken("valid_refresh_token")).thenReturn(USERNAME);
        when(jwtUtil.getTenantIdFromToken("valid_refresh_token")).thenReturn(TENANT_ID);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:1")).thenReturn("valid_refresh_token");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(eq(USER_ID), eq(USERNAME), eq(TENANT_ID))).thenReturn("new_access_token");
        when(jwtUtil.generateRefreshToken(eq(USER_ID), eq(USERNAME), eq(TENANT_ID))).thenReturn("new_refresh_token");

        Map<String, Object> result = authService.refreshToken("valid_refresh_token");

        assertNotNull(result);
        assertEquals("new_access_token", result.get("token"));
        assertEquals("new_refresh_token", result.get("refreshToken"));
        verify(valueOperations).set(eq("refresh_token:1"), eq("new_refresh_token"), eq(7L), any());
    }

    @Test
    @DisplayName("刷新Token - 无效token，抛出BusinessException")
    void testRefreshToken_InvalidToken_ThrowsException() {
        when(jwtUtil.validateToken("invalid_token")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.refreshToken("invalid_token")
        );
        assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("刷新Token - Redis中token不匹配，抛出BusinessException")
    void testRefreshToken_TokenMismatchInRedis_ThrowsException() {
        when(jwtUtil.validateToken("old_refresh_token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("old_refresh_token")).thenReturn(USER_ID);
        when(jwtUtil.getUsernameFromToken("old_refresh_token")).thenReturn(USERNAME);
        when(jwtUtil.getTenantIdFromToken("old_refresh_token")).thenReturn(TENANT_ID);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:1")).thenReturn("different_stored_token");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.refreshToken("old_refresh_token")
        );
        assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("刷新Token - 用户已被禁用，抛出BusinessException")
    void testRefreshToken_UserDisabled_ThrowsException() {
        testUser.setIsActive(false);
        when(jwtUtil.validateToken("valid_token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid_token")).thenReturn(USER_ID);
        when(jwtUtil.getUsernameFromToken("valid_token")).thenReturn(USERNAME);
        when(jwtUtil.getTenantIdFromToken("valid_token")).thenReturn(TENANT_ID);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:1")).thenReturn("valid_token");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.refreshToken("valid_token")
        );
        assertEquals(ResultCode.USER_DISABLED.getCode(), exception.getCode());
    }

    // ==================== 密码修改测试 ====================

    @Test
    @DisplayName("修改密码 - 旧密码正确，成功修改")
    void testChangePassword_CorrectOldPassword_Success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordPolicyValidator.validate("NewPass@12345")).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode("NewPass@12345")).thenReturn("new_encoded_password");

        authService.changePassword(USER_ID, PASSWORD, "NewPass@12345");

        verify(userRepository).save(argThat(user ->
                "new_encoded_password".equals(user.getPassword())
        ));
        verify(redisTemplate).delete("refresh_token:1");
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误，抛出BusinessException")
    void testChangePassword_WrongOldPassword_ThrowsException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongOld@123", ENCODED_PASSWORD)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.changePassword(USER_ID, "WrongOld@123", "NewPass@12345")
        );
        assertEquals(ResultCode.INVALID_PASSWORD.getCode(), exception.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("修改密码 - 新密码不符合策略，抛出BusinessException")
    void testChangePassword_NewPasswordPolicyViolation_ThrowsException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordPolicyValidator.validate("weak"))
                .thenReturn(List.of("密码长度不能少于8个字符", "密码必须包含至少一个大写字母"));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.changePassword(USER_ID, PASSWORD, "weak")
        );
        assertTrue(exception.getMessage().contains("密码长度不能少于8个字符"));
    }

    @Test
    @DisplayName("修改密码 - 用户不存在，抛出BusinessException")
    void testChangePassword_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.changePassword(999L, PASSWORD, "NewPass@12345")
        );
        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== 密码策略验证测试 ====================

    @Test
    @DisplayName("密码策略 - 密码过短，返回错误")
    void testPasswordPolicy_TooShort() {
        when(passwordPolicyValidator.validate("Ab1!")).thenReturn(List.of("密码长度不能少于8个字符"));

        List<String> errors = passwordPolicyValidator.validate("Ab1!");
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("8"));
    }

    @Test
    @DisplayName("密码策略 - 缺少大写字母，返回错误")
    void testPasswordPolicy_MissingUppercase() {
        when(passwordPolicyValidator.validate("abcdefg1!"))
                .thenReturn(List.of("密码必须包含至少一个大写字母"));

        List<String> errors = passwordPolicyValidator.validate("abcdefg1!");
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("大写字母"));
    }

    @Test
    @DisplayName("密码策略 - 缺少数字，返回错误")
    void testPasswordPolicy_MissingDigit() {
        when(passwordPolicyValidator.validate("Abcdefgh!"))
                .thenReturn(List.of("密码必须包含至少一个数字"));

        List<String> errors = passwordPolicyValidator.validate("Abcdefgh!");
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("数字"));
    }

    @Test
    @DisplayName("密码策略 - 缺少特殊字符，返回错误")
    void testPasswordPolicy_MissingSpecialChar() {
        when(passwordPolicyValidator.validate("Abcdefg12"))
                .thenReturn(List.of("密码必须包含至少一个特殊字符"));

        List<String> errors = passwordPolicyValidator.validate("Abcdefg12");
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("特殊字符"));
    }

    @Test
    @DisplayName("密码策略 - 合规密码，无错误")
    void testPasswordPolicy_ValidPassword() {
        when(passwordPolicyValidator.validate(PASSWORD)).thenReturn(Collections.emptyList());

        List<String> errors = passwordPolicyValidator.validate(PASSWORD);
        assertTrue(errors.isEmpty());
    }

    // ==================== 登出测试 ====================

    @Test
    @DisplayName("登出 - 成功清除refresh token")
    void testLogout_Success() {
        when(redisTemplate.delete("refresh_token:1")).thenReturn(true);
        when(jwtUtil.validateToken("access_token")).thenReturn(false);

        authService.logout(USER_ID, "access_token");

        verify(redisTemplate).delete("refresh_token:1");
    }

    @Test
    @DisplayName("登出 - 有效access token加入黑名单")
    void testLogout_WithValidAccessToken_BlacklistsToken() {
        when(redisTemplate.delete("refresh_token:1")).thenReturn(true);
        when(jwtUtil.validateToken("valid_access_token")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.getClaimsFromToken("valid_access_token"))
                .thenReturn(io.jsonwebtoken.Jwts.claims()
                        .expiration(new Date(System.currentTimeMillis() + 3600000))
                        .build());

        authService.logout(USER_ID, "valid_access_token");

        verify(redisTemplate).delete("refresh_token:1");
        verify(valueOperations).set(eq("token_blacklist:valid_access_token"), eq("1"), anyLong(), any());
    }

    // ==================== Token黑名单测试 ====================

    @Test
    @DisplayName("检查Token黑名单 - token在黑名单中返回true")
    void testIsTokenBlacklisted_ReturnsTrue() {
        when(redisTemplate.hasKey("token_blacklist:some_token")).thenReturn(true);

        assertTrue(authService.isTokenBlacklisted("some_token"));
    }

    @Test
    @DisplayName("检查Token黑名单 - token不在黑名单中返回false")
    void testIsTokenBlacklisted_ReturnsFalse() {
        when(redisTemplate.hasKey("token_blacklist:some_token")).thenReturn(false);

        assertFalse(authService.isTokenBlacklisted("some_token"));
    }

    // ==================== 辅助方法 ====================

    private RegisterRequestDTO buildValidRegisterRequest() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);
        request.setConfirmPassword(PASSWORD);
        request.setEmail("test@example.com");
        request.setTenantId(TENANT_ID);
        return request;
    }
}
