package com.aiagent.controller;

import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.Result;
import com.aiagent.exception.RateLimitException;
import com.aiagent.service.FileStorageService;
import com.aiagent.service.FileStorageService.FileInfo;
import com.aiagent.service.QuotaService;
import com.aiagent.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件管理接口")
public class FileController {

    private final FileStorageService fileStorageService;
    private final StringRedisTemplate redisTemplate;
    private final QuotaService quotaService;

    /**
     * Upload a file.
     *
     * @param file   the multipart file to upload
     * @param subDir optional sub-directory for organization
     * @return FileInfo containing metadata about the stored file
     */
    @RequiresPermission("file:upload")
    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    @OperationLog(value = "上传文件", module = "文件管理")
    public Result<FileInfo> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subDir", required = false) String subDir,
            HttpServletRequest httpRequest) {
        // 文件上传限流：每IP每分钟最多20次
        String clientIp = SecurityUtils.getClientIp(httpRequest);
        checkRateLimit("upload:" + clientIp, 20, 60);

        // 存储配额检查
        quotaService.checkStorageQuota(file.getSize());

        // 路径遍历防护
        validateSubDir(subDir);
        log.info("File upload request: name={}, size={}, subDir={}",
                file.getOriginalFilename(), file.getSize(), subDir);
        try {
            FileInfo fileInfo = fileStorageService.upload(file, subDir);
            return Result.success(fileInfo);
        } catch (IllegalArgumentException e) {
            log.warn("File upload validation failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("File upload failed", e);
            return Result.error("File upload failed: " + e.getMessage());
        }
    }

    /**
     * Download a file by its stored path.
     *
     * @param filePath the relative file path
     * @return the file as a downloadable resource
     */
    @RequiresPermission("file:view")
    @GetMapping("/download/{filePath}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(@PathVariable String filePath) {
        log.info("File download request: {}", filePath);
        try {
            Resource resource = fileStorageService.getFile(filePath);
            String contentType = "application/octet-stream";
            String encodedFileName = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (Exception e) {
            log.error("File download failed: {}", filePath, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List files in a directory.
     *
     * @param subDir optional sub-directory to list files from
     * @return list of FileInfo for all files in the directory
     */
    @RequiresPermission("file:read")
    @Operation(summary = "获取文件列表")
    @GetMapping("/list")
    public Result<List<FileInfo>> listFiles(
            @RequestParam(value = "subDir", required = false) String subDir) {
        // 路径遍历防护
        validateSubDir(subDir);
        log.info("List files request: subDir={}", subDir);
        try {
            List<FileInfo> files = fileStorageService.listFiles(subDir);
            return Result.success(files);
        } catch (Exception e) {
            log.error("List files failed", e);
            return Result.error("Failed to list files: " + e.getMessage());
        }
    }

    /**
     * Delete a file by its stored path.
     *
     * @param id the relative file path (used as identifier)
     * @return success or failure result
     */
    @RequiresPermission("file:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    @OperationLog(value = "删除文件", module = "文件管理")
    public Result<Void> delete(@PathVariable String id) {
        log.info("File delete request: {}", id);
        try {
            fileStorageService.delete(id);
            return Result.success();
        } catch (RuntimeException e) {
            log.warn("File delete failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("File delete failed", e);
            return Result.error("File delete failed: " + e.getMessage());
        }
    }

    // ==================== 参数校验辅助方法 ====================

    private void validateSubDir(String subDir) {
        if (subDir != null && (subDir.contains("..") || subDir.contains("\\"))) {
            throw new IllegalArgumentException("非法的子目录路径");
        }
    }

    // ==================== 速率限制辅助方法 ====================

    private void checkRateLimit(String key, int maxAttempts, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
        if (count != null && count > maxAttempts) {
            throw new RateLimitException("请求过于频繁，请稍后重试");
        }
    }
}
