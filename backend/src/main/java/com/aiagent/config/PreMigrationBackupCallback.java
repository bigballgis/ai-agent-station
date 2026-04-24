package com.aiagent.config;

import com.aiagent.config.properties.BackupProperties;
import com.aiagent.service.DatabaseBackupService;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Flyway callback that creates a database backup before running migrations.
 * This provides a safety net in case migrations need to be rolled back.
 */
@Component
public class PreMigrationBackupCallback implements Callback {

    private static final Logger log = LoggerFactory.getLogger(PreMigrationBackupCallback.class);

    private final BackupProperties backupProperties;
    private final DatabaseBackupService databaseBackupService;

    public PreMigrationBackupCallback(BackupProperties backupProperties, DatabaseBackupService databaseBackupService) {
        this.backupProperties = backupProperties;
        this.databaseBackupService = databaseBackupService;
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_MIGRATE && backupProperties.isBackupBeforeMigration();
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        log.info("Pre-migration backup callback triggered");

        if (!backupProperties.isBackupBeforeMigration()) {
            log.info("Pre-migration backup is disabled, skipping");
            return;
        }

        try {
            var result = databaseBackupService.createPreMigrationBackup();
            if (result.getCode() == 200) {
                log.info("Pre-migration backup created successfully: {}", result.getData());
            } else {
                log.warn("Pre-migration backup failed: {}. Continuing with migration.", result.getMessage());
            }
        } catch (Exception e) {
            log.error("Pre-migration backup failed with exception. Continuing with migration.", e);
        }
    }

    @Override
    public String getCallbackName() {
        return "PreMigrationBackupCallback";
    }
}
