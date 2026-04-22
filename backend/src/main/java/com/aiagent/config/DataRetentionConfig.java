package com.aiagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 数据保留策略配置
 * 通过 application.yml 配置各类数据的保留天数和清理定时任务
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "retention")
public class DataRetentionConfig {

    /**
     * 系统日志保留天数，默认90天
     */
    private int logsDays = 90;

    /**
     * 测试结果保留天数，默认180天
     */
    private int testResultsDays = 180;

    /**
     * 审计日志保留天数，默认365天
     */
    private int auditLogsDays = 365;

    /**
     * 登录日志保留天数，默认180天
     */
    private int loginLogsDays = 180;

    /**
     * 清理定时任务Cron表达式，默认每天凌晨3点
     */
    private String cleanupCron = "0 0 3 * * ?";
}
