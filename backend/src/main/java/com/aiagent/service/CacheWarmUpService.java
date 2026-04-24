package com.aiagent.service;

import com.aiagent.repository.DictTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 缓存预热服务
 *
 * 在应用启动完成后，预热高频访问的缓存数据，
 * 减少首次请求的响应时间。
 *
 * 预热策略:
 * - 字典类型列表：几乎所有页面都需要，优先预热
 * - 权限列表：认证和鉴权频繁使用
 * 不预热 Agent 列表等业务数据（数据量大、租户隔离）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmUpService {

    private final DictService dictService;
    private final PermissionService permissionService;

    /**
     * 应用启动完成后执行缓存预热
     * 使用 @EventListener(ApplicationReadyEvent.class) 确保所有 Bean 初始化完成
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        log.info("=== 开始缓存预热 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 预热字典类型列表（几乎所有页面都需要）
            warmUpDictTypes();
        } catch (Exception e) {
            log.warn("字典类型缓存预热失败: {}", e.getMessage());
        }

        try {
            // 2. 预热权限列表（认证和鉴权频繁使用）
            warmUpPermissions();
        } catch (Exception e) {
            log.warn("权限列表缓存预热失败: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("=== 缓存预热完成，耗时: {}ms ===", elapsed);
    }

    private void warmUpDictTypes() {
        log.info("预热字典类型列表...");
        dictService.getAllActiveDictTypes();
        log.info("字典类型列表预热完成");
    }

    private void warmUpPermissions() {
        log.info("预热权限列表...");
        permissionService.getAllPermissions();
        log.info("权限列表预热完成");
    }
}
