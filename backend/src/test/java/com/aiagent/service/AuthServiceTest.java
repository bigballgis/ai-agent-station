package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.User;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.UserRepository;
import com.aiagent.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * 测试用户登录、Token刷新、登出等认证功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 初始化测试用户数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("encoded_password");
        testUser.setEmail("admin@test.com");
        testUser.setPhone("13800138000");
        testUser.setTenantId(100L);
        testUser.setIsActive(true);
    }

    @Test
    @DisplayName("登录 - 成功")
    void testLogin_Success() {
        // 模拟依赖行为
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken(eq(1L), eq("admin"), eq(100L))).thenReturn("access_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 执行登录
        Map<String, Object> result = authService.login("admin", "password123", null);

        // 验证结果
        assertNotNull(result);
        assertEquals("access_token", result.get("token"));
        assertNotNull(result.get("refreshToken"));
        assertNotNull(result.get("user"));

        // 验证 Redis 存储 refresh token
        verify(valueOperations).set(eq("refresh_token:1"), anyString(), eq(7L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("登录 - 带租户ID成功")
    void testLogin_Success_WithTenantId() {
        when(userRepository.findByUsernameAndTenantId("admin", 100L))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken(eq(1L), eq("admin"), eq(100L))).thenReturn("access_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> result = authService.login("admin", "password123", 100L);

        assertNotNull(result);
        assertEquals("access_token", result.get("token"));
        verify(userRepository).findByUsernameAndTenantId("admin", 100L);
    }

    @Test
    @DisplayName("登录 - 密码错误抛出异常")
    void testLogin_WrongPassword() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login("admin", "wrong_password", null);
        });

        assertEquals(ResultCode.INVALID_PASSWORD.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录 - 用户不存在抛出异常")
    void testLogin_UserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login("nonexistent", "password", null);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录 - 用户已禁用抛出异常")
    void testLogin_UserDisabled() {
        testUser.setIsActive(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login("admin", "password123", null);
        });

        assertEquals(ResultCode.USER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("生成Token - 验证Token存在")
    void testGenerateToken() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken(eq(1L), eq("admin"), eq(100L))).thenReturn("generated_token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> result = authService.login("admin", "password123", null);

        assertNotNull(result.get("token"));
        verify(jwtUtil, atLeastOnce()).generateToken(eq(1L), eq("admin"), eq(100L));
    }

    @Test
    @DisplayName("刷新Token - 成功")
    void testRefreshToken() {
        // 模拟 refresh token 验证
        when(jwtUtil.validateToken("valid_refresh_token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid_refresh_token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("valid_refresh_token")).thenReturn("admin");
        when(jwtUtil.getTenantIdFromToken("valid_refresh_token")).thenReturn(100L);

        // 模拟 Redis 中存储的 refresh token
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:1")).thenReturn("valid_refresh_token");

        // 模拟用户仍然有效
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 模拟生成新 token
        when(jwtUtil.generateToken(eq(1L), eq("admin"), eq(100L))).thenReturn("new_access_token");

        // 执行刷新
        Map<String, Object> result = authService.refreshToken("valid_refresh_token");

        // 验证结果
        assertNotNull(result);
        assertEquals("new_access_token", result.get("token"));
        assertNotNull(result.get("refreshToken"));

        // 验证 Redis 中更新了 refresh token
        verify(valueOperations).set(eq("refresh_token:1"), anyString(), eq(7L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("刷新Token - 无效Token抛出异常")
    void testRefreshToken_InvalidToken() {
        when(jwtUtil.validateToken("invalid_token")).thenReturn(false);

        assertThrows(BusinessException.class, () -> {
            authService.refreshToken("invalid_token");
        });
    }

    @Test
    @DisplayName("刷新Token - Redis中Token不匹配抛出异常")
    void testRefreshToken_TokenMismatch() {
        when(jwtUtil.validateToken("old_refresh_token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("old_refresh_token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("old_refresh_token")).thenReturn("admin");
        when(jwtUtil.getTenantIdFromToken("old_refresh_token")).thenReturn(100L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:1")).thenReturn("different_token");

        assertThrows(BusinessException.class, () -> {
            authService.refreshToken("old_refresh_token");
        });
    }

    @Test
    @DisplayName("刷新Token - 用户已被禁用抛出异常")
    void testRefreshToken_UserDisabled() {
        testUser.setIsActive(false);
        when(jwtUtil.validateToken("valid_token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid_token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("valid_token")).thenReturn("admin");
        when(jwtUtil.getTenantIdFromToken("valid_token")).thenReturn(100L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:1")).thenReturn("valid_token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(BusinessException.class, () -> {
            authService.refreshToken("valid_token");
        });
    }

    @Test
    @DisplayName("登出 - 成功清除Token")
    void testLogout() {
        when(redisTemplate.delete("refresh_token:1")).thenReturn(true);

        authService.logout(1L);

        verify(redisTemplate).delete("refresh_token:1");
    }
}
