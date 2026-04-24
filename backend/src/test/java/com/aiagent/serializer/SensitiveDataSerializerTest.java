package com.aiagent.serializer;

import com.aiagent.annotation.SensitiveType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * SensitiveDataSerializer 单元测试
 * 测试敏感数据脱敏序列化器各类型脱敏策略
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("敏感数据序列化器测试")
class SensitiveDataSerializerTest {

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private SerializerProvider serializerProvider;

    // ==================== PASSWORD 类型测试 ====================

    @Test
    @DisplayName("PASSWORD 类型 - 序列化为 ******")
    void testSerialize_PasswordType_MasksCompletely() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PASSWORD, 0, 0);

        serializer.serialize("mySecretPassword123", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("******");
    }

    @Test
    @DisplayName("PASSWORD 类型 - 空密码也掩码")
    void testSerialize_PasswordType_EmptyString_MasksCompletely() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PASSWORD, 0, 0);

        serializer.serialize("", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("******");
    }

    @Test
    @DisplayName("PASSWORD 类型 - 短密码也掩码")
    void testSerialize_PasswordType_ShortPassword_MasksCompletely() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PASSWORD, 0, 0);

        serializer.serialize("ab", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("******");
    }

    // ==================== PARTIAL 类型测试 ====================

    @Test
    @DisplayName("PARTIAL 类型 - 保留前4后4位")
    void testSerialize_PartialType_MasksMiddle() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PARTIAL, 4, 4);

        serializer.serialize("sk-proj-abcdefghijklmnop", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("sk-p****mnop");
    }

    @Test
    @DisplayName("PARTIAL 类型 - 自定义前缀后缀长度")
    void testSerialize_PartialType_CustomPrefixSuffix() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PARTIAL, 2, 6);

        serializer.serialize("abcdefghijklmnopqrstuvwxyz", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("ab****uvwxyz");
    }

    @Test
    @DisplayName("PARTIAL 类型 - 字符串太短不掩码")
    void testSerialize_PartialType_TooShort_NoMask() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PARTIAL, 4, 4);

        serializer.serialize("short", jsonGenerator, serializerProvider);

        // "short" has 5 chars, prefixLen(4) + suffixLen(4) = 8 > 5, so no masking
        verify(jsonGenerator).writeString("short");
    }

    // ==================== EMAIL 类型测试 ====================

    @Test
    @DisplayName("EMAIL 类型 - 正常邮箱脱敏")
    void testSerialize_EmailType_NormalEmail() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.EMAIL, 0, 0);

        serializer.serialize("user@example.com", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("u***@example.com");
    }

    @Test
    @DisplayName("EMAIL 类型 - 长前缀邮箱脱敏")
    void testSerialize_EmailType_LongPrefixEmail() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.EMAIL, 0, 0);

        serializer.serialize("longusername123@example.com", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("l***@example.com");
    }

    @Test
    @DisplayName("EMAIL 类型 - 不含@符号不脱敏")
    void testSerialize_EmailType_NoAtSign_NoMask() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.EMAIL, 0, 0);

        serializer.serialize("notanemail", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("notanemail");
    }

    // ==================== PHONE 类型测试 ====================

    @Test
    @DisplayName("PHONE 类型 - 正常手机号脱敏")
    void testSerialize_PhoneType_NormalPhone() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PHONE, 0, 0);

        serializer.serialize("13812345678", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("138****5678");
    }

    @Test
    @DisplayName("PHONE 类型 - 带区号的手机号脱敏")
    void testSerialize_PhoneType_WithCountryCode() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PHONE, 0, 0);

        serializer.serialize("+8613812345678", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("+86****5678");
    }

    @Test
    @DisplayName("PHONE 类型 - 短号码不脱敏")
    void testSerialize_PhoneType_TooShort_NoMask() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PHONE, 0, 0);

        serializer.serialize("123", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("123");
    }

    // ==================== null 值测试 ====================

    @Test
    @DisplayName("null 值 - 输出 null")
    void testSerialize_NullValue_WritesNull() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(SensitiveType.PASSWORD, 0, 0);

        serializer.serialize(null, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeNull();
    }

    // ==================== 默认构造函数测试 ====================

    @Test
    @DisplayName("默认构造函数 - PASSWORD 类型")
    void testDefaultConstructor_PasswordType() throws Exception {
        SensitiveDataSerializer serializer = new SensitiveDataSerializer();

        serializer.serialize("anyPassword", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("******");
    }
}
