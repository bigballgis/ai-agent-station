package com.aiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * Configuration for file upload security constraints.
 * Defines allowed file types, size limits, and security settings.
 */
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    /** Maximum file size in bytes (default: 50MB) */
    private long maxFileSize = 50 * 1024 * 1024;

    /** Whether virus scanning is enabled */
    private boolean virusScanEnabled = false;

    /** Allowed file extensions (whitelist) */
    private Set<String> allowedExtensions = new HashSet<>(Set.of(
            // Images
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico",
            // Documents
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf",
            // Archives
            "zip", "rar", "7z", "tar", "gz",
            // Code / Config
            "json", "xml", "yaml", "yml", "properties",
            // Media
            "mp3", "mp4", "avi", "mov", "wav"
    ));

    /** Allowed MIME types (whitelist) */
    private Set<String> allowedMimeTypes = new HashSet<>(Set.of(
            // Images
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg+xml", "image/x-icon",
            // Documents
            "application/pdf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/csv", "application/rtf",
            // Archives
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
            "application/x-tar", "application/gzip",
            // Code / Config
            "application/json", "application/xml", "text/xml", "text/yaml",
            // Media
            "audio/mpeg", "audio/wav", "video/mp4", "video/avi", "video/quicktime"
    ));

    /** Maximum file size per extension (in bytes). Extensions not listed fall back to maxFileSize. */
    private Map<String, Long> maxFileSizePerType = new HashMap<>();

    /** Blocked file extensions (blacklist, takes precedence over whitelist) */
    private Set<String> blockedExtensions = new HashSet<>(Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "class",
            "jsp", "php", "asp", "aspx", "cgi", "pl", "py", "rb",
            "dll", "so", "dylib", "sys", "com", "scr", "pif", "lnk"
    ));

    // ==================== Getters and Setters ====================

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public boolean isVirusScanEnabled() {
        return virusScanEnabled;
    }

    public void setVirusScanEnabled(boolean virusScanEnabled) {
        this.virusScanEnabled = virusScanEnabled;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(Set<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public Set<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(Set<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public Map<String, Long> getMaxFileSizePerType() {
        return maxFileSizePerType;
    }

    public void setMaxFileSizePerType(Map<String, Long> maxFileSizePerType) {
        this.maxFileSizePerType = maxFileSizePerType;
    }

    public Set<String> getBlockedExtensions() {
        return blockedExtensions;
    }

    public void setBlockedExtensions(Set<String> blockedExtensions) {
        this.blockedExtensions = blockedExtensions;
    }

    /**
     * Get the maximum allowed file size for a given extension.
     */
    public long getMaxFileSizeForExtension(String extension) {
        return maxFileSizePerType.getOrDefault(extension.toLowerCase(), maxFileSize);
    }
}
