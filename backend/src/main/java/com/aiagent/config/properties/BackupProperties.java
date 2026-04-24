package com.aiagent.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Database backup configuration properties.
 * Corresponds to app.backup.* configuration in application.yml.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.backup")
public class BackupProperties {

    /** Whether automatic backup is enabled */
    private boolean enabled = true;

    /** Directory path for storing backup files */
    @NotBlank
    private String storagePath = "/data/backups";

    /** Maximum number of backup files to retain */
    @Min(1)
    @Max(365)
    private int retentionCount = 30;

    /** Backup file name prefix */
    @NotBlank
    private String filePrefix = "aiagent_backup";

    /** Cron expression for scheduled backups (default: daily at 2 AM) */
    @NotBlank
    private String scheduleCron = "0 0 2 * * ?";

    /** Whether to compress backup files using gzip */
    private boolean compress = true;

    /** Timeout in seconds for backup/restore operations */
    @Min(30)
    @Max(3600)
    private int timeoutSeconds = 600;

    /** Whether to create a backup before Flyway migrations */
    private boolean backupBeforeMigration = true;
}
