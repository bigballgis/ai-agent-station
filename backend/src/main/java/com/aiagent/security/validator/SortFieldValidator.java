package com.aiagent.security.validator;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 排序字段白名单验证器。
 * 防止用户通过 sortBy 参数注入任意 JPQL/HQL 字段名。
 */
@Component
public class SortFieldValidator {

    /**
     * 验证排序字段是否在允许的白名单内。
     * 仅允许字母、数字、下划线组成的字段名，防止 JPQL 注入。
     *
     * @param sortBy       用户传入的排序字段
     * @param allowedFields 允许的字段白名单
     * @return 验证后的安全排序字段
     * @throws IllegalArgumentException 如果字段不在白名单中
     */
    public String validate(String sortBy, Set<String> allowedFields) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        String trimmed = sortBy.trim();
        // 基本字符安全检查：只允许字母、数字、下划线、点号
        if (!trimmed.matches("^[a-zA-Z0-9_.]+$")) {
            throw new IllegalArgumentException("非法的排序字段: " + trimmed);
        }
        if (allowedFields != null && !allowedFields.isEmpty() && !allowedFields.contains(trimmed)) {
            throw new IllegalArgumentException("不支持的排序字段: " + trimmed + "，允许的字段: " + allowedFields);
        }
        return trimmed;
    }

    /**
     * 验证排序方向。
     *
     * @param direction 排序方向
     * @return 安全的排序方向（"asc" 或 "desc"）
     */
    public String validateDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return "desc";
        }
        String trimmed = direction.trim().toLowerCase();
        if (!"asc".equals(trimmed) && !"desc".equals(trimmed)) {
            throw new IllegalArgumentException("非法的排序方向: " + direction + "，仅允许 asc 或 desc");
        }
        return trimmed;
    }

    /**
     * 验证分页参数，确保 page >= 0 且 1 <= size <= maxSize。
     *
     * @param page   页码
     * @param size   每页大小
     * @param maxSize 最大允许的每页大小
     * @return 修正后的安全分页参数 [page, size]
     */
    public int[] validatePagination(int page, int size, int maxSize) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), maxSize);
        return new int[]{safePage, safeSize};
    }
}
