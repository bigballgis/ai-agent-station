package com.aiagent.security.validator;

import com.aiagent.config.FileUploadConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validator for file uploads that enforces security policies:
 * - Extension whitelist and blacklist checking
 * - MIME type validation
 * - File size limits per type
 * - Path traversal detection
 * - Double extension attack detection
 */
@Component
public class FileUploadValidator {

    private final FileUploadConfig fileUploadConfig;

    public FileUploadValidator(FileUploadConfig fileUploadConfig) {
        this.fileUploadConfig = fileUploadConfig;
    }

    /**
     * Validate a multipart file upload against all security rules.
     *
     * @param file the uploaded file
     * @return list of validation error messages (empty if valid)
     */
    public List<String> validate(MultipartFile file) {
        List<String> errors = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add("File is empty or not provided");
            return errors;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            errors.add("File name is missing");
            return errors;
        }

        // 1. Check for null bytes in filename
        if (originalFilename.contains("\0")) {
            errors.add("File name contains null bytes (potential injection attack)");
        }

        // 2. Check for path traversal attacks
        String pathTraversalError = checkPathTraversal(originalFilename);
        if (pathTraversalError != null) {
            errors.add(pathTraversalError);
        }

        // 3. Check for double extension attacks (e.g., file.jsp.png)
        String doubleExtError = checkDoubleExtension(originalFilename);
        if (doubleExtError != null) {
            errors.add(doubleExtError);
        }

        // 4. Extract and validate extension
        String extension = getExtension(originalFilename);
        if (extension == null || extension.isBlank()) {
            errors.add("File has no extension");
        } else {
            // Check against blocked extensions (blacklist takes precedence)
            if (fileUploadConfig.getBlockedExtensions().contains(extension.toLowerCase())) {
                errors.add("File extension '" + extension + "' is blocked for security reasons");
            }

            // Check against allowed extensions (whitelist)
            if (!fileUploadConfig.getAllowedExtensions().contains(extension.toLowerCase())) {
                errors.add("File extension '" + extension + "' is not in the allowed list");
            }

            // 5. Check file size for this specific type
            long maxSize = fileUploadConfig.getMaxFileSizeForExtension(extension);
            if (file.getSize() > maxSize) {
                errors.add("File size (" + formatSize(file.getSize()) + ") exceeds the maximum allowed size (" +
                        formatSize(maxSize) + ") for ." + extension + " files");
            }
        }

        // 6. Check MIME type
        String contentType = file.getContentType();
        if (contentType != null && !fileUploadConfig.getAllowedMimeTypes().contains(contentType)) {
            errors.add("MIME type '" + contentType + "' is not allowed");
        }

        // 7. Cross-check extension vs MIME type for common mismatches
        if (extension != null && contentType != null) {
            String mismatchError = checkExtensionMimeTypeMismatch(extension, contentType);
            if (mismatchError != null) {
                errors.add(mismatchError);
            }
        }

        return errors;
    }

    /**
     * Check for path traversal sequences in the filename.
     */
    private String checkPathTraversal(String filename) {
        String normalized = filename.replace('\\', '/');

        if (normalized.contains("../") || normalized.contains("..\\") ||
                normalized.contains("/..") || normalized.contains("\\..") ||
                normalized.startsWith("..") || normalized.endsWith("..")) {
            return "File name contains path traversal sequence (..)";
        }

        // Check for absolute paths
        if (normalized.startsWith("/") || normalized.matches("^[a-zA-Z]:.*")) {
            return "File name appears to be an absolute path";
        }

        return null;
    }

    /**
     * Check for double extension attacks (e.g., file.jsp.png, file.php.jpg).
     * A double extension is when the second-to-last extension is a dangerous executable type.
     */
    private String checkDoubleExtension(String filename) {
        String baseName = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;

        if (!baseName.contains(".")) {
            return null;
        }

        // Get the second-to-last extension
        String secondExtension = baseName.substring(baseName.lastIndexOf('.') + 1).toLowerCase();

        // Check if the second extension is in the blocked list
        if (fileUploadConfig.getBlockedExtensions().contains(secondExtension)) {
            return "Potential double extension attack detected: '" + filename +
                    "'. The extension '" + secondExtension + "' is blocked";
        }

        return null;
    }

    /**
     * Check for suspicious extension/MIME type mismatches.
     */
    private String checkExtensionMimeTypeMismatch(String extension, String contentType) {
        extension = extension.toLowerCase();

        // Image extensions should have image MIME types
        if (Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "ico").contains(extension)) {
            if (!contentType.startsWith("image/")) {
                return "Suspicious MIME type '" + contentType + "' for ." + extension + " file";
            }
        }

        // PDF should have application/pdf
        if ("pdf".equals(extension) && !"application/pdf".equals(contentType)) {
            return "Suspicious MIME type '" + contentType + "' for .pdf file";
        }

        // Document types
        if (Set.of("doc", "docx").contains(extension)) {
            if (!contentType.contains("wordprocessingml") && !"application/msword".equals(contentType)) {
                return "Suspicious MIME type '" + contentType + "' for ." + extension + " file";
            }
        }

        if (Set.of("xls", "xlsx").contains(extension)) {
            if (!contentType.contains("spreadsheetml") && !"application/vnd.ms-excel".equals(contentType)) {
                return "Suspicious MIME type '" + contentType + "' for ." + extension + " file";
            }
        }

        return null;
    }

    /**
     * Extract the file extension from a filename.
     */
    private String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    /**
     * Format a file size in bytes to a human-readable string.
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        double value = bytes / Math.pow(1024, exp);
        return String.format("%.1f %sB", value, unit);
    }
}
