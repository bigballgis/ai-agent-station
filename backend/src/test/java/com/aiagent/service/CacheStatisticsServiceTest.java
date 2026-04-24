package com.aiagent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheStatisticsService 单元测试
 * 测试缓存命中率统计功能
 */
@DisplayName("缓存统计服务测试")
class CacheStatisticsServiceTest {

    private CacheStatisticsService cacheStatisticsService;

    @BeforeEach
    void setUp() {
        cacheStatisticsService = new CacheStatisticsService();
    }

    // ==================== recordHit 测试 ====================

    @Test
    @DisplayName("记录缓存命中 - 首次命中")
    void testRecordHit_FirstHit() {
        cacheStatisticsService.recordHit("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");
        assertEquals(1, stats.getHits().get());
        assertEquals(0, stats.getMisses().get());
    }

    @Test
    @DisplayName("记录缓存命中 - 多次命中累加")
    void testRecordHit_MultipleHits() {
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordHit("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");
        assertEquals(3, stats.getHits().get());
    }

    // ==================== recordMiss 测试 ====================

    @Test
    @DisplayName("记录缓存未命中 - 首次未命中")
    void testRecordMiss_FirstMiss() {
        cacheStatisticsService.recordMiss("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");
        assertEquals(0, stats.getHits().get());
        assertEquals(1, stats.getMisses().get());
    }

    @Test
    @DisplayName("记录缓存未命中 - 多次未命中累加")
    void testRecordMiss_MultipleMisses() {
        cacheStatisticsService.recordMiss("dict");
        cacheStatisticsService.recordMiss("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");
        assertEquals(2, stats.getMisses().get());
    }

    // ==================== recordEviction 测试 ====================

    @Test
    @DisplayName("记录缓存驱逐 - 首次驱逐")
    void testRecordEviction_FirstEviction() {
        cacheStatisticsService.recordEviction("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");
        assertEquals(1, stats.getEvictions().get());
    }

    @Test
    @DisplayName("记录缓存驱逐 - 多次驱逐累加")
    void testRecordEviction_MultipleEvictions() {
        cacheStatisticsService.recordEviction("dict");
        cacheStatisticsService.recordEviction("dict");
        cacheStatisticsService.recordEviction("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");
        assertEquals(3, stats.getEvictions().get());
    }

    // ==================== getStats 测试 ====================

    @Test
    @DisplayName("获取所有缓存统计 - 空统计")
    void testGetStats_All_Empty() {
        var allStats = cacheStatisticsService.getStats();

        assertTrue(allStats.isEmpty());
    }

    @Test
    @DisplayName("获取所有缓存统计 - 多个缓存区域")
    void testGetStats_All_MultipleRegions() {
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordMiss("user");
        cacheStatisticsService.recordHit("agent");

        var allStats = cacheStatisticsService.getStats();

        assertEquals(3, allStats.size());
        assertTrue(allStats.containsKey("dict"));
        assertTrue(allStats.containsKey("user"));
        assertTrue(allStats.containsKey("agent"));
    }

    @Test
    @DisplayName("获取指定缓存统计 - 不存在的区域返回空统计")
    void testGetStats_NonExistentRegion_ReturnsEmptyStats() {
        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("nonexistent");

        assertNotNull(stats);
        assertEquals(0, stats.getHits().get());
        assertEquals(0, stats.getMisses().get());
        assertEquals(0, stats.getEvictions().get());
    }

    @Test
    @DisplayName("获取指定缓存统计 - 已存在的区域返回正确统计")
    void testGetStats_ExistingRegion_ReturnsCorrectStats() {
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordMiss("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");

        assertEquals(2, stats.getHits().get());
        assertEquals(1, stats.getMisses().get());
    }

    // ==================== resetStats 测试 ====================

    @Test
    @DisplayName("重置所有统计 - 清空所有缓存区域")
    void testResetStats_ClearsAll() {
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordMiss("user");
        cacheStatisticsService.recordEviction("agent");

        cacheStatisticsService.resetStats();

        assertTrue(cacheStatisticsService.getStats().isEmpty());
    }

    @Test
    @DisplayName("重置指定缓存区域统计 - 只清除目标区域")
    void testResetStats_SpecificRegion_OnlyClearsTarget() {
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordMiss("user");

        cacheStatisticsService.resetStats("dict");

        assertNull(cacheStatisticsService.getStats().get("dict"));
        assertNotNull(cacheStatisticsService.getStats().get("user"));
        assertEquals(1, cacheStatisticsService.getStats("user").getMisses().get());
    }

    @Test
    @DisplayName("重置不存在的缓存区域 - 不影响其他区域")
    void testResetStats_NonExistentRegion_NoEffect() {
        cacheStatisticsService.recordHit("dict");

        cacheStatisticsService.resetStats("nonexistent");

        assertEquals(1, cacheStatisticsService.getStats("dict").getHits().get());
    }

    // ==================== 混合操作测试 ====================

    @Test
    @DisplayName("混合操作 - 命中和未命中交替记录")
    void testMixedOperations_HitAndMissAlternating() {
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordMiss("dict");
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordMiss("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");

        assertEquals(3, stats.getHits().get());
        assertEquals(2, stats.getMisses().get());
    }

    @Test
    @DisplayName("重置后重新记录 - 统计从零开始")
    void testResetAndRecord_Again() {
        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordMiss("dict");

        cacheStatisticsService.resetStats();

        cacheStatisticsService.recordHit("dict");
        cacheStatisticsService.recordHit("dict");

        CacheStatisticsService.CacheStats stats = cacheStatisticsService.getStats("dict");

        assertEquals(2, stats.getHits().get());
        assertEquals(0, stats.getMisses().get());
    }
}
