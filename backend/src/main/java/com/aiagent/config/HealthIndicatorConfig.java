package com.aiagent.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;

/**
 * 自定义健康指标配置
 * 提供 Database、Redis、DiskSpace 的健康检查
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HealthIndicatorConfig {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    /**
     * 数据库连接健康检查
     * 执行简单查询验证数据库连通性
     */
    @Bean
    public HealthIndicator databaseConnectionHealth() {
        return () -> {
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(3)) {
                    var meta = conn.getMetaData();
                    return Health.up()
                            .withDetail("database", meta.getDatabaseProductName())
                            .withDetail("url", meta.getURL())
                            .withDetail("driver", meta.getDriverName() + " " + meta.getDriverVersion())
                            .build();
                }
                return Health.down().withDetail("error", "Connection validation failed").build();
            } catch (Exception e) {
                log.error("Database health check failed", e);
                return Health.down()
                        .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Redis 连接健康检查
     * 使用 PING 命令验证 Redis 连通性
     */
    @Bean
    public HealthIndicator redisConnectionHealth() {
        return () -> {
            try {
                String result = redisTemplate.getConnectionFactory().getConnection().ping();
                if ("PONG".equalsIgnoreCase(result)) {
                    return Health.up()
                            .withDetail("status", "PONG")
                            .build();
                }
                return Health.down().withDetail("response", result).build();
            } catch (Exception e) {
                log.error("Redis health check failed", e);
                return Health.down()
                        .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                        .build();
            }
        };
    }

    /**
     * 磁盘空间健康检查
     * 检查可用磁盘空间，阈值 500MB
     */
    @Bean
    public HealthIndicator diskSpaceHealth() {
        return () -> {
            try {
                FileStore store = Files.getFileStore(Paths.get("/").toAbsolutePath());
                long totalSpace = store.getTotalSpace();
                long freeSpace = store.getUsableSpace();
                long freeSpaceMb = freeSpace / (1024 * 1024);
                long totalSpaceMb = totalSpace / (1024 * 1024);
                long thresholdMb = 500;

                Health.Builder builder;
                if (freeSpaceMb >= thresholdMb) {
                    builder = Health.up();
                } else {
                    builder = Health.down();
                }

                return builder
                        .withDetail("freeSpaceMb", freeSpaceMb)
                        .withDetail("totalSpaceMb", totalSpaceMb)
                        .withDetail("thresholdMb", thresholdMb)
                        .withDetail("percentFree", String.format("%.1f%%", (double) freeSpace / totalSpace * 100))
                        .build();
            } catch (Exception e) {
                log.error("Disk space health check failed", e);
                return Health.down()
                        .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                        .build();
            }
        };
    }
}
