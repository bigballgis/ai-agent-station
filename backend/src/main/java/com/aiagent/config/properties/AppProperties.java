package com.aiagent.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 文件存储和应用线程池配置属性
 *
 * 对应 application.yml 中 app.* 前缀的配置项。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 文件存储配置 */
    private Storage storage = new Storage();

    /** 线程池配置 */
    private ThreadPool threadPool = new ThreadPool();

    @Data
    public static class Storage {
        /** 文件存储路径 */
        @NotBlank
        private String path = "/data/uploads";
    }

    @Data
    public static class ThreadPool {
        /** 核心线程数 */
        @Min(1)
        private int coreSize = Runtime.getRuntime().availableProcessors();

        /** 最大线程数 */
        @Min(1)
        private int maxSize = Runtime.getRuntime().availableProcessors() * 2;

        /** 队列容量 */
        @Min(1)
        private int queueCapacity = 200;

        /** 空闲线程存活时间（秒） */
        @Min(1)
        private int keepAliveSeconds = 60;

        /** 线程名前缀 */
        @NotBlank
        private String threadNamePrefix = "ai-agent-";
    }
}
