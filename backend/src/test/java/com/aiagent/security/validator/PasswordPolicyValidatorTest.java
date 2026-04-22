package com.aiagent.security.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordPolicyValidator 单元测试
 * 测试密码安全策略验证功能
 */
@DisplayName("密码策略验证器测试")
class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator();
    }

    @Test
    @DisplayName("合法密码 - 验证通过")
    void testValidPassword() {
        String password = "Test@12345";
        assertTrue(validator.isValid(password));
        List<String> errors = validator.validate(password);
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("密码过短 - 返回错误")
    void testTooShort() {
        String password = "Ab1@";
        assertFalse(validator.isValid(password));
        List<String> errors = validator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("不能少于8个字符")));
    }

    @Test
    @DisplayName("无大写字母 - 返回错误")
    void testNoUpperCase() {
        String password = "test@12345";
        assertFalse(validator.isValid(password));
        List<String> errors = validator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("大写字母")));
    }

    @Test
    @DisplayName("无小写字母 - 返回错误")
    void testNoLowerCase() {
        String password = "TEST@12345";
        assertFalse(validator.isValid(password));
        List<String> errors = validator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("小写字母")));
    }

    @Test
    @DisplayName("无数字 - 返回错误")
    void testNoDigit() {
        String password = "Test@abcdef";
        assertFalse(validator.isValid(password));
        List<String> errors = validator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("数字")));
    }

    @Test
    @DisplayName("无特殊字符 - 返回错误")
    void testNoSpecialChar() {
        String password = "Test12345678";
        assertFalse(validator.isValid(password));
        List<String> errors = validator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("特殊字符")));
    }

    @Test
    @DisplayName("常见弱密码 - 返回错误")
    void testCommonPassword() {
        // 即使满足长度和字符要求，常见密码仍应被拒绝
        // 注意：常见密码列表中的密码通常不满足复杂度要求
        // 但我们测试 isCommonPassword 的逻辑
        String password = "Password1!";
        assertFalse(validator.isValid(password));
        List<String> errors = validator.validate(password);
        assertTrue(errors.stream().anyMatch(e -> e.contains("过于简单")));
    }

    @Test
    @DisplayName("null密码 - 返回错误")
    void testNullPassword() {
        assertFalse(validator.isValid(null));
        List<String> errors = validator.validate(null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("不能少于")));
    }

    @Test
    @DisplayName("密码过长 - 返回错误")
    void testTooLong() {
        String password = "A" + "a1@".repeat(50); // 超过128个字符
        assertFalse(validator.isValid(password));
        List<String> errors = validator.validate(password);
        assertTrue(errors.stream().anyMatch(e -> e.contains("不能超过")));
    }

    @Test
    @DisplayName("多种错误同时存在 - 返回多个错误")
    void testMultipleErrors() {
        String password = "abc";
        List<String> errors = validator.validate(password);
        // 应该有：太短、无大写、无数字、无特殊字符
        assertTrue(errors.size() >= 3);
    }

    @Test
    @DisplayName("边界情况 - 恰好8位合法密码")
    void testExactlyMinLength() {
        String password = "Test@123";
        assertTrue(validator.isValid(password));
    }
}
