package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.service.CacheStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 缓存统计端点
 *
 * 提供缓存命中率、命中/未命中次数等统计信息，
 * 用于性能监控和缓存策略调优。
 */
@RestController
@RequestMapping("/v1/cache-stats")
@RequiredArgsConstructor
@Tag(name = "缓存统计", description = "缓存命中率统计接口")
public class CacheStatsController {

    private final CacheStatisticsService cacheStatisticsService;

    @GetMapping
    @Operation(summary = "获取缓存统计信息", description = "返回各缓存区域的命中/未命中次数和命中率")
    public Result<Map<String, CacheStatisticsService.CacheStats>> getCacheStats() {
        return Result.success(cacheStatisticsService.getStats());
    }
}
