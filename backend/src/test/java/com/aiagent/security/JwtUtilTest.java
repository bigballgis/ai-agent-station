package com.aiagent.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 * 测试 JWT Token 的生成、验证、解析等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 工具类测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "this-is-a-very-long-secret-key-for-testing-jwt-at-least-32-chars";
    private static final Long TEST_USER_ID = 100L;
    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1800000L);  // 30 minutes
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L);  // 7 days
    }

    // ==================== generateToken 测试 ====================

    @Test
    @DisplayName("生成 Access Token - 成功")
    void testGenerateToken_Success() {
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);  // JWT 格式: header.payload.signature
    }

    @Test
    @DisplayName("生成 Access Token - 包含正确的 Claims")
    void testGenerateToken_ContainsCorrectClaims() {
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);
        Claims claims = jwtUtil.getClaimsFromToken(token);

        assertEquals(TEST_USERNAME, claims.getSubject());
        assertEquals(TEST_USER_ID, claims.get("userId", Long.class));
        assertEquals(TEST_TENANT_ID, claims.get("tenantId", Long.class));
        assertEquals("access", claims.get("type"));
        assertEquals("aegisnexus", claims.getIssuer());
    }

    // ==================== generateRefreshToken 测试 ====================

    @Test
    @DisplayName("生成 Refresh Token - 成功")
    void testGenerateRefreshToken_Success() {
        String token = jwtUtil.generateRefreshToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("生成 Refresh Token - 类型为 refresh")
    void testGenerateRefreshToken_TypeIsRefresh() {
        String token = jwtUtil.generateRefreshToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);
        Claims claims = jwtUtil.getClaimsFromToken(token);

        assertEquals("refresh", claims.get("type"));
    }

    // ==================== validateToken 测试 ====================

    @Test
    @DisplayName("验证有效 Token - 返回 true")
    void testValidateToken_ValidToken_ReturnsTrue() {
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("验证过期 Token - 返回 false")
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);  // 已过期
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", -1000L);

        String expiredToken = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    @DisplayName("验证无效签名 Token - 返回 false")
    void testValidateToken_InvalidSignature_ReturnsFalse() {
        // 使用不同的密钥生成 token
        String otherSecret = "another-very-long-secret-key-that-is-different-from-the-original-one";
        SecretKey otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));

        String invalidToken = Jwts.builder()
                .issuer("aegisnexus")
                .audience().add("aegisnexus-client").and()
                .subject(TEST_USERNAME)
                .claim("userId", TEST_USER_ID)
                .claim("tenantId", TEST_TENANT_ID)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    @DisplayName("验证畸形 Token - 返回 false")
    void testValidateToken_MalformedToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken("not.a.valid.jwt.token"));
    }

    @Test
    @DisplayName("验证空 Token - 返回 false")
    void testValidateToken_EmptyToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    @DisplayName("验证 null Token - 返回 false")
    void testValidateToken_NullToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    @DisplayName("验证随机字符串 Token - 返回 false")
    void testValidateToken_RandomString_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken("randomstring12345"));
    }

    // ==================== getUsernameFromToken 测试 ====================

    @Test
    @DisplayName("从 Token 提取用户名 - 成功")
    void testGetUsernameFromToken_Success() {
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertEquals(TEST_USERNAME, jwtUtil.getUsernameFromToken(token));
    }

    // ==================== getUserIdFromToken 测试 ====================

    @Test
    @DisplayName("从 Token 提取用户ID - 成功")
    void testGetUserIdFromToken_Success() {
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertEquals(TEST_USER_ID, jwtUtil.getUserIdFromToken(token));
    }

    // ==================== getTenantIdFromToken 测试 ====================

    @Test
    @DisplayName("从 Token 提取租户ID - 成功")
    void testGetTenantIdFromToken_Success() {
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertEquals(TEST_TENANT_ID, jwtUtil.getTenantIdFromToken(token));
    }

    // ==================== isTokenExpired 测试 ====================

    @Test
    @DisplayName("检查未过期 Token - 返回 false")
    void testIsTokenExpired_NotExpired_ReturnsFalse() {
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    @DisplayName("检查过期 Token - 返回 true")
    void testIsTokenExpired_ExpiredToken_ReturnsTrue() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", -1000L);

        String expiredToken = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertTrue(jwtUtil.isTokenExpired(expiredToken));
    }

    // ==================== isRefreshToken 测试 ====================

    @Test
    @DisplayName("判断 Refresh Token - 返回 true")
    void testIsRefreshToken_RefreshToken_ReturnsTrue() {
        String token = jwtUtil.generateRefreshToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertTrue(jwtUtil.isRefreshToken(token));
    }

    @Test
    @DisplayName("判断 Access Token - 返回 false")
    void testIsRefreshToken_AccessToken_ReturnsFalse() {
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_TENANT_ID);

        assertFalse(jwtUtil.isRefreshToken(token));
    }

    @Test
    @DisplayName("判断无效 Token - 返回 false")
    void testIsRefreshToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtUtil.isRefreshToken("invalid.token.here"));
    }

    // ==================== validateSecret 测试 ====================

    @Test
    @DisplayName("验证密钥 - 密钥过短抛出异常")
    void testValidateSecret_ShortSecret_ThrowsException() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", "short");

        assertThrows(IllegalStateException.class, util::validateSecret);
    }

    @Test
    @DisplayName("验证密钥 - null 密钥抛出异常")
    void testValidateSecret_NullSecret_ThrowsException() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", null);

        assertThrows(IllegalStateException.class, util::validateSecret);
    }

    @Test
    @DisplayName("验证密钥 - 合法密钥不抛出异常")
    void testValidateSecret_ValidSecret_NoException() {
        assertDoesNotThrow(jwtUtil::validateSecret);
    }
}
