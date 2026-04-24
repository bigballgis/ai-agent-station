package com.aiagent.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MessageUtils 单元测试
 * 测试国际化消息解析功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("消息工具类测试")
class MessageUtilsTest {

    @Mock
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        // 使用反射设置静态 messageSource
        try {
            var field = MessageUtils.class.getDeclaredField("messageSource");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        // 清理静态 messageSource
        try {
            var field = MessageUtils.class.getDeclaredField("messageSource");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== getMessage (带 Locale) 测试 ====================

    @Test
    @DisplayName("获取消息 - 存在的消息码")
    void testGetMessage_ExistingCode_ReturnsMessage() {
        // 通过构造函数设置静态 messageSource
        new MessageUtils(messageSource);
        when(messageSource.getMessage(eq("user.not.found"), any(), any(Locale.class)))
                .thenReturn("用户不存在");

        String result = MessageUtils.getMessage("user.not.found", Locale.SIMPLIFIED_CHINESE);

        assertEquals("用户不存在", result);
        verify(messageSource).getMessage(eq("user.not.found"), any(), eq(Locale.SIMPLIFIED_CHINESE));
    }

    @Test
    @DisplayName("获取消息 - 带参数的消息码")
    void testGetMessage_WithArgs_ReturnsFormattedMessage() {
        new MessageUtils(messageSource);
        when(messageSource.getMessage(eq("user.name.required"), eq(new Object[]{"用户名"}), any(Locale.class)))
                .thenReturn("用户名不能为空");

        String result = MessageUtils.getMessage("user.name.required", Locale.SIMPLIFIED_CHINESE, "用户名");

        assertEquals("用户名不能为空", result);
    }

    @Test
    @DisplayName("获取消息 - 多个参数")
    void testGetMessage_WithMultipleArgs_ReturnsFormattedMessage() {
        new MessageUtils(messageSource);
        when(messageSource.getMessage(eq("field.min.length"), eq(new Object[]{"密码", 8}), any(Locale.class)))
                .thenReturn("密码长度不能少于8个字符");

        String result = MessageUtils.getMessage("field.min.length", Locale.SIMPLIFIED_CHINESE, "密码", 8);

        assertEquals("密码长度不能少于8个字符", result);
    }

    @Test
    @DisplayName("获取消息 - 缺失的消息码返回消息码本身")
    void testGetMessage_MissingCode_ReturnsCode() {
        new MessageUtils(messageSource);
        when(messageSource.getMessage(eq("nonexistent.code"), any(), any(Locale.class)))
                .thenThrow(new org.springframework.context.NoSuchMessageException("nonexistent.code"));

        String result = MessageUtils.getMessage("nonexistent.code", Locale.SIMPLIFIED_CHINESE);

        assertEquals("nonexistent.code", result);
    }

    @Test
    @DisplayName("获取消息 - MessageSource 抛出异常返回消息码本身")
    void testGetMessage_Exception_ReturnsCode() {
        new MessageUtils(messageSource);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenThrow(new RuntimeException("unexpected error"));

        String result = MessageUtils.getMessage("error.code", Locale.SIMPLIFIED_CHINESE);

        assertEquals("error.code", result);
    }

    // ==================== getMessage (无 Locale, 使用上下文) 测试 ====================

    @Test
    @DisplayName("获取消息 - 使用当前 Locale 上下文")
    void testGetMessage_WithContextLocale() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        new MessageUtils(messageSource);
        when(messageSource.getMessage(eq("success"), any(), eq(Locale.SIMPLIFIED_CHINESE)))
                .thenReturn("操作成功");

        String result = MessageUtils.getMessage("success");

        assertEquals("操作成功", result);
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("获取消息 - 英文 Locale")
    void testGetMessage_EnglishLocale() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        new MessageUtils(messageSource);
        when(messageSource.getMessage(eq("success"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Operation successful");

        String result = MessageUtils.getMessage("success");

        assertEquals("Operation successful", result);
        LocaleContextHolder.resetLocaleContext();
    }

    // ==================== MessageSource 为 null 测试 ====================

    @Test
    @DisplayName("获取消息 - MessageSource 未初始化返回消息码")
    void testGetMessage_NullMessageSource_ReturnsCode() {
        // messageSource 为 null（未通过构造函数设置）
        String result = MessageUtils.getMessage("any.code");

        assertEquals("any.code", result);
    }

    @Test
    @DisplayName("获取消息 - MessageSource 未初始化带参数返回消息码")
    void testGetMessage_NullMessageSource_WithArgs_ReturnsCode() {
        String result = MessageUtils.getMessage("any.code", Locale.SIMPLIFIED_CHINESE, "arg1", "arg2");

        assertEquals("any.code", result);
    }

    // ==================== 构造函数测试 ====================

    @Test
    @DisplayName("构造函数 - 正确设置 MessageSource")
    void testConstructor_SetsMessageSource() {
        assertNull(getStaticMessageSource());

        new MessageUtils(messageSource);

        assertNotNull(getStaticMessageSource());
    }

    private Object getStaticMessageSource() {
        try {
            var field = MessageUtils.class.getDeclaredField("messageSource");
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
