package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "分页请求")
public class PageRequest {

    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "页码(从0开始)", example = "0")
    @Min(value = 0, message = "页码不能小于0")
    private int page = 0;

    @Schema(description = "每页大小", example = "20")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private int size = 20;

    @Schema(description = "排序字段", example = "createdAt")
    private String sortBy;

    @Schema(description = "排序方向", example = "desc")
    private String sortDir = "desc";

    /**
     * 转换为Spring Data的Pageable对象
     * 自动将 size 限制在 1-100 范围内
     */
    public org.springframework.data.domain.Pageable toPageable() {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        if (sortBy == null || sortBy.isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(safePage, safeSize);
        }
        org.springframework.data.domain.Sort.Direction direction;
        try {
            direction = org.springframework.data.domain.Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException e) {
            direction = org.springframework.data.domain.Sort.Direction.DESC;
        }
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(direction, sortBy);
        return org.springframework.data.domain.PageRequest.of(safePage, safeSize, sort);
    }
}
