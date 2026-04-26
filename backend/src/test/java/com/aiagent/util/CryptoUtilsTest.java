package com.aiagent.util;

import com.aiagent.config.properties.AiAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CryptoUtils 单元测试
 * 测试 AES-256-GCM 加密/解密功能
 */
@DisplayName("加密工具类测试")
class CryptoUtilsTest {

    private CryptoUtils cryptoUtils;

    private static final String TEST_SECRET_KEY = "this-is-a-32-byte-secret-key-for-test!!";

    private static CryptoUtils newCryptoUtils(String secretKey) {
        AiAgentProperties props = new AiAgentProperties();
        props.getCrypto().setSecretKey(secretKey);
        return new CryptoUtils(props);
    }

    @BeforeEach
    void setUp() {
        cryptoUtils = newCryptoUtils(TEST_SECRET_KEY);
    }

    // ==================== encrypt / decrypt 基本功能测试 ====================

    @Test
    @DisplayName("加密后解密 - 返回原始明文")
    void testEncryptAndDecrypt_ReturnsOriginal() {
        String plainText = "Hello, World!";
        String encrypted = cryptoUtils.encrypt(plainText);
        String decrypted = cryptoUtils.decrypt(encrypted);

        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("加密 - 返回 Base64 编码字符串")
    void testEncrypt_ReturnsBase64String() {
        String encrypted = cryptoUtils.encrypt("test");

        assertNotNull(encrypted);
        // Base64 字符串只包含字母、数字、+、/、=
        assertTrue(encrypted.matches("^[A-Za-z0-9+/=]+$"));
    }

    @Test
    @DisplayName("加密相同明文两次 - 结果不同（随机 IV）")
    void testEncrypt_SameInput_DifferentOutput() {
        String plainText = "same input text";

        String encrypted1 = cryptoUtils.encrypt(plainText);
        String encrypted2 = cryptoUtils.encrypt(plainText);

        assertNotEquals(encrypted1, encrypted2);
    }

    // ==================== null / 空字符串边界测试 ====================

    @Test
    @DisplayName("加密 null - 返回 null")
    void testEncrypt_NullInput_ReturnsNull() {
        assertNull(cryptoUtils.encrypt(null));
    }

    @Test
    @DisplayName("加密空字符串 - 返回空字符串")
    void testEncrypt_EmptyInput_ReturnsEmpty() {
        assertEquals("", cryptoUtils.encrypt(""));
    }

    @Test
    @DisplayName("解密 null - 返回 null")
    void testDecrypt_NullInput_ReturnsNull() {
        assertNull(cryptoUtils.decrypt(null));
    }

    @Test
    @DisplayName("解密空字符串 - 返回空字符串")
    void testDecrypt_EmptyInput_ReturnsEmpty() {
        assertEquals("", cryptoUtils.decrypt(""));
    }

    // ==================== 错误密钥测试 ====================

    @Test
    @DisplayName("使用不同密钥解密 - 抛出异常")
    void testDecrypt_WrongKey_ThrowsException() {
        CryptoUtils otherCrypto = newCryptoUtils("another-different-secret-key-for-testing!");

        String encrypted = cryptoUtils.encrypt("secret data");

        assertThrows(RuntimeException.class, () -> otherCrypto.decrypt(encrypted));
    }

    @Test
    @DisplayName("解密无效 Base64 字符串 - 抛出异常")
    void testDecrypt_InvalidBase64_ThrowsException() {
        assertThrows(RuntimeException.class, () -> cryptoUtils.decrypt("not-valid-base64!!!"));
    }

    @Test
    @DisplayName("解密被篡改的密文 - 抛出异常")
    void testDecrypt_TamperedCipherText_ThrowsException() {
        String encrypted = cryptoUtils.encrypt("sensitive data");
        // 篡改密文
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "XX";

        assertThrows(RuntimeException.class, () -> cryptoUtils.decrypt(tampered));
    }

    // ==================== 长文本测试 ====================

    @Test
    @DisplayName("加密解密长文本 - 成功")
    void testEncryptDecrypt_LongText_Success() {
        String longText = "A".repeat(10000);
        String encrypted = cryptoUtils.encrypt(longText);
        String decrypted = cryptoUtils.decrypt(encrypted);

        assertEquals(longText, decrypted);
    }

    // ==================== 特殊字符测试 ====================

    @Test
    @DisplayName("加密解密特殊字符 - 成功")
    void testEncryptDecrypt_SpecialCharacters_Success() {
        String specialText = "密码: P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?`~\n\t\r";
        String encrypted = cryptoUtils.encrypt(specialText);
        String decrypted = cryptoUtils.decrypt(encrypted);

        assertEquals(specialText, decrypted);
    }

    // ==================== Unicode 测试 ====================

    @Test
    @DisplayName("加密解密中文文本 - 成功")
    void testEncryptDecrypt_ChineseText_Success() {
        String chineseText = "你好世界！这是一段中文测试文本。";
        String encrypted = cryptoUtils.encrypt(chineseText);
        String decrypted = cryptoUtils.decrypt(encrypted);

        assertEquals(chineseText, decrypted);
    }

    // ==================== validateSecretKey 测试 ====================

    @Test
    @DisplayName("验证密钥 - 空密钥抛出异常")
    void testValidateSecretKey_EmptyKey_ThrowsException() {
        CryptoUtils util = newCryptoUtils("");
        assertThrows(IllegalStateException.class, util::validateSecretKey);
    }

    @Test
    @DisplayName("验证密钥 - 默认密钥抛出异常")
    void testValidateSecretKey_DefaultKey_ThrowsException() {
        CryptoUtils util = newCryptoUtils("default-secret-key-change-in-production!!");
        assertThrows(IllegalStateException.class, util::validateSecretKey);
    }

    @Test
    @DisplayName("验证密钥 - 自定义密钥不抛出异常")
    void testValidateSecretKey_CustomKey_NoException() {
        assertDoesNotThrow(cryptoUtils::validateSecretKey);
    }
}
