package com.aiagent.service;

import com.aiagent.common.Result;
import com.aiagent.config.properties.BackupProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

/**
 * Database Backup and Recovery Service.
 * Provides backup creation, listing, restoration, and scheduling using pg_dump/pg_restore.
 */
@Service
public class DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final BackupProperties backupProperties;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledBackupFuture;

    public DatabaseBackupService(BackupProperties backupProperties, TaskScheduler taskScheduler) {
        this.backupProperties = backupProperties;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        // Ensure backup directory exists
        try {
            Path backupDir = Paths.get(backupProperties.getStoragePath());
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
                log.info("Created backup directory: {}", backupDir);
            }
        } catch (IOException e) {
            log.error("Failed to create backup directory: {}", backupProperties.getStoragePath(), e);
        }

        // Schedule automatic backups if enabled
        if (backupProperties.isEnabled()) {
            scheduleBackup(backupProperties.getScheduleCron());
        }
    }

    /**
     * Create a database backup using pg_dump
     */
    public Result<Map<String, Object>> createBackup() {
        log.info("Starting database backup...");

        if (!backupProperties.isEnabled()) {
            return Result.error(400, "Database backup is disabled");
        }

        String backupId = "backup_" + LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String fileName = backupProperties.getFilePrefix() + "_" + backupId;
        String backupPath = backupProperties.getStoragePath() + File.separator + fileName;

        try {
            // Build pg_dump command
            List<String> command = buildPgDumpCommand(backupPath);

            log.info("Executing pg_dump command...");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(backupProperties.getTimeoutSeconds(), java.util.concurrent.TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                // Clean up partial file
                cleanupFile(backupPath);
                cleanupFile(backupPath + ".gz");
                return Result.error(500, "Backup timed out after " + backupProperties.getTimeoutSeconds() + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // Clean up partial file
                cleanupFile(backupPath);
                cleanupFile(backupPath + ".gz");
                log.error("pg_dump failed with exit code {}: {}", exitCode, output);
                return Result.error(500, "Backup failed with exit code " + exitCode);
            }

            // Compress if enabled
            String finalPath = backupPath;
            if (backupProperties.isCompress()) {
                finalPath = compressFile(backupPath);
            }

            // Verify backup file
            if (!verifyBackupFile(finalPath)) {
                cleanupFile(finalPath);
                return Result.error(500, "Backup verification failed - file may be corrupted");
            }

            // Apply retention policy
            applyRetentionPolicy();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("backupId", backupId);
            result.put("filePath", finalPath);
            result.put("fileSize", formatFileSize(Files.size(Path.of(finalPath))));
            result.put("createdAt", LocalDateTime.now().toString());
            result.put("compressed", backupProperties.isCompress());

            log.info("Database backup completed successfully: {}", backupId);
            return Result.success("Backup created successfully", result);

        } catch (IOException e) {
            log.error("Failed to create backup", e);
            return Result.error(500, "Backup failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Backup interrupted", e);
            return Result.error(500, "Backup interrupted");
        }
    }

    /**
     * Create a pre-migration backup (used by Flyway callback)
     */
    public Result<String> createPreMigrationBackup() {
        log.info("Creating pre-migration backup...");
        Result<Map<String, Object>> result = createBackup();
        if (result.getCode() == 200 && result.getData() != null) {
            return Result.success((String) result.getData().get("backupId"));
        }
        return Result.error(result.getCode(), "Pre-migration backup failed: " + result.getMessage());
    }

    /**
     * List available backups
     */
    public Result<List<Map<String, Object>>> listBackups() {
        log.info("Listing available backups...");

        try {
            Path backupDir = Paths.get(backupProperties.getStoragePath());
            if (!Files.exists(backupDir)) {
                return Result.success(new ArrayList<>());
            }

            List<Map<String, Object>> backups = new ArrayList<>();

            try (Stream<Path> files = Files.list(backupDir)) {
                files.filter(path -> {
                    String name = path.getFileName().toString();
                    return name.startsWith(backupProperties.getFilePrefix()) &&
                           (name.endsWith(".sql") || name.endsWith(".sql.gz") || name.endsWith(".dump") || name.endsWith(".dump.gz"));
                })
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Map<String, Object> info = new LinkedHashMap<>();
                        String fileName = path.getFileName().toString();
                        info.put("fileName", fileName);
                        info.put("filePath", path.toString());
                        info.put("fileSize", formatFileSize(Files.size(path)));
                        info.put("lastModified", Files.getLastModifiedTime(path).toString());
                        info.put("compressed", fileName.endsWith(".gz"));

                        // Extract backup ID from filename
                        String backupId = extractBackupId(fileName);
                        info.put("backupId", backupId);

                        backups.add(info);
                    } catch (IOException e) {
                        log.warn("Failed to read backup file info: {}", path, e);
                    }
                });
            }

            return Result.success(backups);

        } catch (IOException e) {
            log.error("Failed to list backups", e);
            return Result.error(500, "Failed to list backups: " + e.getMessage());
        }
    }

    /**
     * Restore from a backup
     */
    public Result<Map<String, Object>> restoreBackup(String backupId) {
        log.warn("Starting database restore from backup: {}", backupId);

        String backupFilePath = findBackupFile(backupId);
        if (backupFilePath == null) {
            return Result.error(404, "Backup not found: " + backupId);
        }

        // Create a pre-restore backup for safety
        log.info("Creating pre-restore safety backup...");
        Result<Map<String, Object>> preBackup = createBackup();
        String preBackupId = preBackup.getData() != null ? (String) preBackup.getData().get("backupId") : "unknown";

        try {
            // Build restore command
            List<String> command = buildRestoreCommand(backupFilePath);

            log.info("Executing restore command...");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Restore output: {}", line);
                }
            }

            boolean completed = process.waitFor(backupProperties.getTimeoutSeconds(), java.util.concurrent.TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                return Result.error(500, "Restore timed out after " + backupProperties.getTimeoutSeconds() + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Restore failed with exit code {}: {}", exitCode, output);
                return Result.error(500, "Restore failed with exit code " + exitCode);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("backupId", backupId);
            result.put("restoredAt", LocalDateTime.now().toString());
            result.put("preRestoreBackupId", preBackupId);
            result.put("status", "SUCCESS");

            log.info("Database restore completed successfully from backup: {}", backupId);
            return Result.success("Database restored successfully", result);

        } catch (IOException e) {
            log.error("Failed to restore backup", e);
            return Result.error(500, "Restore failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Restore interrupted", e);
            return Result.error(500, "Restore interrupted");
        }
    }

    /**
     * Schedule automatic backups using a cron expression
     */
    public Result<Map<String, Object>> scheduleBackup(String cronExpression) {
        try {
            // Validate cron expression
            CronExpression.parse(cronExpression);

            // Cancel existing schedule
            if (scheduledBackupFuture != null && !scheduledBackupFuture.isCancelled()) {
                scheduledBackupFuture.cancel(false);
            }

            // Schedule new backup
            CronTrigger trigger = new CronTrigger(cronExpression);
            scheduledBackupFuture = taskScheduler.schedule(
                    this::createBackup,
                    trigger
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("cronExpression", cronExpression);
            result.put("enabled", true);
            result.put("nextExecution", trigger.nextExecutionTime(new Date()));

            log.info("Automatic backup scheduled with cron: {}", cronExpression);
            return Result.success("Backup schedule updated", result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid cron expression: {}", cronExpression, e);
            return Result.error(400, "Invalid cron expression: " + e.getMessage());
        }
    }

    /**
     * Verify backup file integrity
     */
    private boolean verifyBackupFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                return false;
            }

            long fileSize = Files.size(path);
            if (fileSize == 0) {
                log.warn("Backup file is empty: {}", filePath);
                return false;
            }

            // For compressed files, verify gzip header
            if (filePath.endsWith(".gz")) {
                try (InputStream is = Files.newInputStream(path)) {
                    byte[] header = new byte[2];
                    int read = is.read(header);
                    if (read < 2 || header[0] != (byte) 0x1F || header[1] != (byte) 0x8B) {
                        log.warn("Backup file is not a valid gzip file: {}", filePath);
                        return false;
                    }
                }
            }

            // For SQL files, check for PostgreSQL header
            if (filePath.endsWith(".sql")) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    String firstLine = reader.readLine();
                    if (firstLine == null || (!firstLine.contains("--") && !firstLine.contains("PostgreSQL"))) {
                        log.warn("Backup SQL file does not appear to be a valid pg_dump output: {}", filePath);
                        return false;
                    }
                }
            }

            return true;

        } catch (IOException e) {
            log.error("Failed to verify backup file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Apply retention policy - delete old backups
     */
    private void applyRetentionPolicy() {
        try {
            Path backupDir = Paths.get(backupProperties.getStoragePath());
            if (!Files.exists(backupDir)) {
                return;
            }

            List<Path> backupFiles;
            try (Stream<Path> files = Files.list(backupDir)) {
                backupFiles = files
                        .filter(path -> {
                            String name = path.getFileName().toString();
                            return name.startsWith(backupProperties.getFilePrefix());
                        })
                        .sorted(Comparator.reverseOrder())
                        .toList();
            }

            if (backupFiles.size() > backupProperties.getRetentionCount()) {
                List<Path> toDelete = backupFiles.subList(backupProperties.getRetentionCount(), backupFiles.size());
                for (Path file : toDelete) {
                    try {
                        Files.deleteIfExists(file);
                        log.info("Deleted old backup (retention policy): {}", file.getFileName());
                    } catch (IOException e) {
                        log.warn("Failed to delete old backup: {}", file, e);
                    }
                }
            }

        } catch (IOException e) {
            log.error("Failed to apply retention policy", e);
        }
    }

    /**
     * Build pg_dump command
     */
    private List<String> buildPgDumpCommand(String outputPath) {
        List<String> command = new ArrayList<>();
        command.add("pg_dump");

        // Database connection from environment variables or defaults
        String dbHost = System.getenv().getOrDefault("SPRING_DATASOURCE_URL_HOST", "localhost");
        String dbPort = System.getenv().getOrDefault("SPRING_DATASOURCE_URL_PORT", "5432");
        String dbName = System.getenv().getOrDefault("SPRING_DATASOURCE_URL_DATABASE", "aiagent");
        String dbUser = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "postgres");

        command.add("-h");
        command.add(dbHost);
        command.add("-p");
        command.add(dbPort);
        command.add("-U");
        command.add(dbUser);
        command.add("-d");
        command.add(dbName);

        // pg_dump options
        command.add("-F"); // Format
        command.add("c");  // Custom format (supports pg_restore)
        command.add("-v"); // Verbose
        command.add("-f");
        command.add(outputPath);

        return command;
    }

    /**
     * Build restore command using pg_restore
     */
    private List<String> buildRestoreCommand(String backupFilePath) {
        List<String> command = new ArrayList<>();
        command.add("pg_restore");

        String dbHost = System.getenv().getOrDefault("SPRING_DATASOURCE_URL_HOST", "localhost");
        String dbPort = System.getenv().getOrDefault("SPRING_DATASOURCE_URL_PORT", "5432");
        String dbName = System.getenv().getOrDefault("SPRING_DATASOURCE_URL_DATABASE", "aiagent");
        String dbUser = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "postgres");

        command.add("-h");
        command.add(dbHost);
        command.add("-p");
        command.add(dbPort);
        command.add("-U");
        command.add(dbUser);
        command.add("-d");
        command.add(dbName);

        // pg_restore options
        command.add("-v");        // Verbose
        command.add("-c");        // Clean (drop) database objects before recreating
        command.add("--if-exists"); // Use IF EXISTS when dropping objects
        command.add("-1");        // Single transaction
        command.add(backupFilePath);

        return command;
    }

    /**
     * Compress a file using gzip
     */
    private String compressFile(String filePath) throws IOException {
        String compressedPath = filePath + ".gz";
        try (InputStream in = Files.newInputStream(Path.of(filePath));
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(Path.of(compressedPath)))) {
            // Use ProcessBuilder with gzip for better compression
            ProcessBuilder pb = new ProcessBuilder("gzip", "-c", filePath);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            try (InputStream gzipIn = process.getInputStream();
                 OutputStream fileOut = Files.newOutputStream(Path.of(compressedPath))) {
                gzipIn.transferTo(fileOut);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("gzip compression failed with exit code " + exitCode);
            }

            // Delete original uncompressed file
            Files.deleteIfExists(Path.of(filePath));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Compression interrupted", e);
        }
        return compressedPath;
    }

    /**
     * Find backup file by backup ID
     */
    private String findBackupFile(String backupId) {
        try {
            Path backupDir = Paths.get(backupProperties.getStoragePath());
            if (!Files.exists(backupDir)) {
                return null;
            }

            try (Stream<Path> files = Files.list(backupDir)) {
                return files
                        .filter(path -> path.getFileName().toString().contains(backupId))
                        .findFirst()
                        .map(Path::toString)
                        .orElse(null);
            }
        } catch (IOException e) {
            log.error("Failed to find backup file: {}", backupId, e);
            return null;
        }
    }

    /**
     * Extract backup ID from filename
     */
    private String extractBackupId(String fileName) {
        // Remove prefix and extension
        String name = fileName;
        if (name.startsWith(backupProperties.getFilePrefix() + "_")) {
            name = name.substring(backupProperties.getFilePrefix().length() + 1);
        }
        // Remove .sql, .gz, .dump extensions
        name = name.replaceAll("\\.(sql|gz|dump|sql\\.gz|dump\\.gz)$", "");
        return name;
    }

    /**
     * Clean up a file if it exists
     */
    private void cleanupFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            log.warn("Failed to cleanup file: {}", filePath, e);
        }
    }

    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
