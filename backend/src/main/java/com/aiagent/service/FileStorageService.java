package com.aiagent.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "text/markdown",
            "application/json",
            "application/xml",
            "text/xml",
            "application/zip",
            "application/x-tar",
            "application/gzip",
            "audio/mpeg",
            "audio/wav",
            "audio/ogg",
            "video/mp4",
            "video/webm",
            "video/quicktime"
    );

    @Value("${app.storage.path:/data/uploads}")
    private String storagePath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize file storage location: " + rootLocation, e);
        }
    }

    /**
     * Upload a file to the storage.
     *
     * @param file   the multipart file to upload
     * @param subDir optional sub-directory (can be null or empty)
     * @return FileInfo containing metadata about the stored file
     */
    public FileInfo upload(MultipartFile file, String subDir) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("File name must not be empty");
        }

        String dateDir = LocalDate.now().toString();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String storedName = uuid + "_" + originalName;

        Path targetDir = rootLocation;
        if (subDir != null && !subDir.trim().isEmpty()) {
            targetDir = targetDir.resolve(subDir.trim());
            // 路径遍历防护
            Path normalizedDir = targetDir.normalize();
            if (!normalizedDir.startsWith(rootLocation)) {
                throw new SecurityException("非法的子目录路径: " + subDir);
            }
            // subDir字符白名单验证
            if (!subDir.matches("^[a-zA-Z0-9_\\-/]+$")) {
                throw new SecurityException("子目录包含非法字符: " + subDir);
            }
            targetDir = normalizedDir;
        }
        targetDir = targetDir.resolve(dateDir);

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = rootLocation.relativize(targetFile).toString();

            log.info("File uploaded successfully: {} -> {}", originalName, relativePath);

            return FileInfo.builder()
                    .id(UUID.randomUUID().toString())
                    .originalName(originalName)
                    .storedName(storedName)
                    .path(relativePath)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .build();
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalName, e);
            throw new RuntimeException("Failed to store file: " + originalName, e);
        }
    }

    /**
     * Delete a file by its relative path.
     *
     * @param filePath the relative file path
     */
    public void delete(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path must not be empty");
        }

        Path fileToDelete = rootLocation.resolve(filePath).normalize();
        if (!fileToDelete.startsWith(rootLocation)) {
            throw new IllegalArgumentException("Invalid file path: path traversal detected");
        }

        try {
            boolean deleted = Files.deleteIfExists(fileToDelete);
            if (deleted) {
                log.info("File deleted successfully: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
                throw new RuntimeException("File not found: " + filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }

    /**
     * Get a file as a Resource by its relative path.
     *
     * @param filePath the relative file path
     * @return Resource for the file
     */
    public Resource getFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path must not be empty");
        }

        Path fileToLoad = rootLocation.resolve(filePath).normalize();
        if (!fileToLoad.startsWith(rootLocation)) {
            throw new IllegalArgumentException("Invalid file path: path traversal detected");
        }

        try {
            Resource resource = new UrlResource(fileToLoad.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.warn("File not found or not readable: {}", filePath);
                throw new RuntimeException("File not found: " + filePath);
            }
        } catch (MalformedURLException e) {
            log.error("Failed to load file: {}", filePath, e);
            throw new RuntimeException("Failed to load file: " + filePath, e);
        }
    }

    /**
     * List all files in a sub-directory.
     *
     * @param subDir optional sub-directory (can be null or empty)
     * @return list of FileInfo for all files in the directory
     */
    public List<FileInfo> listFiles(String subDir) {
        Path targetDir = rootLocation;
        if (subDir != null && !subDir.trim().isEmpty()) {
            targetDir = targetDir.resolve(subDir.trim()).normalize();
        }

        if (!targetDir.startsWith(rootLocation)) {
            throw new IllegalArgumentException("Invalid sub-directory: path traversal detected");
        }

        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            log.warn("Directory does not exist: {}", targetDir);
            return Collections.emptyList();
        }

        try (Stream<Path> paths = Files.walk(targetDir, 5)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            String relativePath = rootLocation.relativize(path).toString();
                            String fileName = path.getFileName().toString();
                            long size = Files.size(path);
                            String contentType = Files.probeContentType(path);
                            return FileInfo.builder()
                                    .id(UUID.nameUUIDFromBytes(relativePath.getBytes()).toString())
                                    .originalName(fileName)
                                    .storedName(fileName)
                                    .path(relativePath)
                                    .size(size)
                                    .contentType(contentType != null ? contentType : "application/octet-stream")
                                    .uploadedAt(LocalDateTime.ofInstant(
                                            Files.getLastModifiedTime(path).toInstant(),
                                            java.time.ZoneId.systemDefault()))
                                    .build();
                        } catch (IOException e) {
                            log.warn("Failed to read file metadata: {}", path, e);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list files in directory: {}", targetDir, e);
            throw new RuntimeException("Failed to list files in directory: " + targetDir, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 50MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "File type not allowed: " + contentType + ". Allowed types: " + ALLOWED_CONTENT_TYPES);
        }
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class FileInfo {
        private String id;
        private String originalName;
        private String storedName;
        private String path;
        private long size;
        private String contentType;
        private LocalDateTime uploadedAt;
    }
}
