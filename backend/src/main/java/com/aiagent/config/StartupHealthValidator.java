package com.aiagent.config;

import com.aiagent.service.ApplicationHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时关键依赖验证
 *
 * 在应用完全启动后，验证数据库和 Redis 等关键依赖是否可用。
 * 如果关键依赖不可用，记录错误日志并发出告警。
 *
 * 注意: 此检查在 ApplicationReadyEvent 之后执行，此时应用已完全初始化。
 * 对于真正的 fail-fast 行为，依赖 Spring Boot 的自动配置失败机制
 * （如 DataSource bean 创建失败会阻止启动）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupHealthValidator {

    private final ApplicationHealthService applicationHealthService;

    /**
     * 在应用完全启动后执行健康检查
     * 使用较低优先级，确保在其他启动逻辑之后执行
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100) // 低优先级，在其他启动事件处理器之后执行
    public void validateOnStartup() {
        log.info("=== 执行启动后健康检查 ===");
        try {
            applicationHealthService.validateCriticalDependencies();
            log.info("=== 启动后健康检查通过 ===");
        } catch (IllegalStateException e) {
            // 记录严重错误日志，但不阻止已启动的应用
            // 在生产环境中，这应该触发告警
            log.error("=== 启动后健康检查失败 ===");
            log.error("关键依赖不可用: {}", e.getMessage());
            log.error("建议: 请检查数据库和 Redis 连接配置，并确保相关服务正在运行。");
            log.error("================================");
        }
    }
}
