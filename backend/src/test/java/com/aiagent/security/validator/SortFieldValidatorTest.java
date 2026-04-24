package com.aiagent.security.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SortFieldValidator 单元测试
 * 测试排序字段白名单验证、排序方向验证、分页参数验证
 */
@DisplayName("排序字段验证器测试")
class SortFieldValidatorTest {

    private SortFieldValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SortFieldValidator();
    }

    // ==================== validate (排序字段) 测试 ====================

    @Test
    @DisplayName("验证合法排序字段 - 在白名单中")
    void testValidate_ValidField_InWhitelist() {
        Set<String> allowed = Set.of("name", "createdAt", "updatedAt");
        String result = validator.validate("name", allowed);

        assertEquals("name", result);
    }

    @Test
    @DisplayName("验证 null 排序字段 - 返回默认值 createdAt")
    void testValidate_NullField_ReturnsDefault() {
        Set<String> allowed = Set.of("name", "createdAt");
        String result = validator.validate(null, allowed);

        assertEquals("createdAt", result);
    }

    @Test
    @DisplayName("验证空白排序字段 - 返回默认值 createdAt")
    void testValidate_BlankField_ReturnsDefault() {
        Set<String> allowed = Set.of("name", "createdAt");
        String result = validator.validate("   ", allowed);

        assertEquals("createdAt", result);
    }

    @Test
    @DisplayName("验证空字符串排序字段 - 返回默认值 createdAt")
    void testValidate_EmptyField_ReturnsDefault() {
        Set<String> allowed = Set.of("name", "createdAt");
        String result = validator.validate("", allowed);

        assertEquals("createdAt", result);
    }

    @Test
    @DisplayName("验证不在白名单中的字段 - 抛出异常")
    void testValidate_FieldNotInWhitelist_ThrowsException() {
        Set<String> allowed = Set.of("name", "createdAt");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("dangerousField", allowed));
        assertTrue(ex.getMessage().contains("不支持的排序字段"));
    }

    @Test
    @DisplayName("验证空白名单 - 任何合法字段名都通过")
    void testValidate_EmptyWhitelist_AnyFieldPasses() {
        String result = validator.validate("anyField", Set.of());

        assertEquals("anyField", result);
    }

    @Test
    @DisplayName("验证 null 白名单 - 任何合法字段名都通过")
    void testValidate_NullWhitelist_AnyFieldPasses() {
        String result = validator.validate("anyField", null);

        assertEquals("anyField", result);
    }

    // ==================== SQL 注入防护测试 ====================

    @Test
    @DisplayName("SQL 注入 - SELECT 语句")
    void testValidate_SqlInjectionSelect_ThrowsException() {
        Set<String> allowed = Set.of("name");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("name; SELECT * FROM users", allowed));
    }

    @Test
    @DisplayName("SQL 注入 - UNION 注入")
    void testValidate_SqlInjectionUnion_ThrowsException() {
        Set<String> allowed = Set.of("name");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("name UNION SELECT * FROM users", allowed));
    }

    @Test
    @DisplayName("SQL 注入 - DROP TABLE")
    void testValidate_SqlInjectionDropTable_ThrowsException() {
        Set<String> allowed = Set.of("name");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("name; DROP TABLE users", allowed));
    }

    @Test
    @DisplayName("SQL 注入 - 单引号注入")
    void testValidate_SqlInjectionQuote_ThrowsException() {
        Set<String> allowed = Set.of("name");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("name' OR '1'='1", allowed));
    }

    @Test
    @DisplayName("SQL 注入 - 双横线注释")
    void testValidate_SqlInjectionComment_ThrowsException() {
        Set<String> allowed = Set.of("name");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("name--", allowed));
    }

    @Test
    @DisplayName("SQL 注入 - 空格注入")
    void testValidate_SqlInjectionSpace_ThrowsException() {
        Set<String> allowed = Set.of("name");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("name OR 1=1", allowed));
    }

    // ==================== 合法特殊字符测试 ====================

    @Test
    @DisplayName("合法字段名 - 包含下划线")
    void testValidate_FieldWithUnderscore_Valid() {
        Set<String> allowed = Set.of("created_at", "updated_at");
        String result = validator.validate("created_at", allowed);

        assertEquals("created_at", result);
    }

    @Test
    @DisplayName("合法字段名 - 包含点号（嵌套属性）")
    void testValidate_FieldWithDot_Valid() {
        Set<String> allowed = Set.of("user.name", "user.email");
        String result = validator.validate("user.name", allowed);

        assertEquals("user.name", result);
    }

    @Test
    @DisplayName("合法字段名 - 纯数字")
    void testValidate_NumericField_Valid() {
        Set<String> allowed = Set.of("123");
        String result = validator.validate("123", allowed);

        assertEquals("123", result);
    }

    // ==================== validateDirection (排序方向) 测试 ====================

    @Test
    @DisplayName("验证排序方向 - asc")
    void testValidateDirection_Asc() {
        assertEquals("asc", validator.validateDirection("asc"));
    }

    @Test
    @DisplayName("验证排序方向 - ASC 大写")
    void testValidateDirection_AscUpperCase() {
        assertEquals("asc", validator.validateDirection("ASC"));
    }

    @Test
    @DisplayName("验证排序方向 - desc")
    void testValidateDirection_Desc() {
        assertEquals("desc", validator.validateDirection("desc"));
    }

    @Test
    @DisplayName("验证排序方向 - DESC 大写")
    void testValidateDirection_DescUpperCase() {
        assertEquals("desc", validator.validateDirection("DESC"));
    }

    @Test
    @DisplayName("验证排序方向 - null 返回默认值 desc")
    void testValidateDirection_Null_ReturnsDefault() {
        assertEquals("desc", validator.validateDirection(null));
    }

    @Test
    @DisplayName("验证排序方向 - 空白返回默认值 desc")
    void testValidateDirection_Blank_ReturnsDefault() {
        assertEquals("desc", validator.validateDirection("   "));
    }

    @Test
    @DisplayName("验证排序方向 - 非法值抛出异常")
    void testValidateDirection_Invalid_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateDirection("invalid"));
    }

    // ==================== validatePagination (分页参数) 测试 ====================

    @Test
    @DisplayName("验证分页参数 - 正常值")
    void testValidatePagination_NormalValues() {
        int[] result = validator.validatePagination(1, 10, 100);

        assertEquals(1, result[0]);
        assertEquals(10, result[1]);
    }

    @Test
    @DisplayName("验证分页参数 - 负页码修正为0")
    void testValidatePagination_NegativePage_CorrectedToZero() {
        int[] result = validator.validatePagination(-5, 10, 100);

        assertEquals(0, result[0]);
    }

    @Test
    @DisplayName("验证分页参数 - size为0修正为1")
    void testValidatePagination_ZeroSize_CorrectedToOne() {
        int[] result = validator.validatePagination(0, 0, 100);

        assertEquals(1, result[1]);
    }

    @Test
    @DisplayName("验证分页参数 - size超过maxSize修正为maxSize")
    void testValidatePagination_SizeExceedsMax_CorrectedToMax() {
        int[] result = validator.validatePagination(0, 500, 100);

        assertEquals(100, result[1]);
    }

    @Test
    @DisplayName("验证分页参数 - 负size修正为1")
    void testValidatePagination_NegativeSize_CorrectedToOne() {
        int[] result = validator.validatePagination(0, -10, 100);

        assertEquals(1, result[1]);
    }

    @Test
    @DisplayName("验证分页参数 - 边界值 maxSize")
    void testValidatePagination_SizeEqualsMax() {
        int[] result = validator.validatePagination(0, 100, 100);

        assertEquals(100, result[1]);
    }
}
