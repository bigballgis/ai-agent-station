package com.aiagent.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 工作流引擎配置属性
 *
 * 对应 application.yml 中 workflow.* 前缀的配置项。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {

    /** 工作流执行最大时长（秒），超时自动标记为 FAILED */
    @Min(10)
    private int maxExecutionDuration = 300;

    /** 工作流定义最大节点数 */
    @Min(1)
    private int maxNodeCount = 50;

    /** 工作流定义最大边数 */
    @Min(1)
    private int maxEdgeCount = 100;
}
