package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页请求")
public class PageRequest {

    @Schema(description = "页码(从0开始)", example = "0")
    private int page = 0;

    @Schema(description = "每页大小", example = "20")
    private int size = 20;

    @Schema(description = "排序字段", example = "createdAt")
    private String sortBy;

    @Schema(description = "排序方向", example = "desc")
    private String sortDir = "desc";

    /**
     * 转换为Spring Data的Pageable对象
     */
    public org.springframework.data.domain.Pageable toPageable() {
        if (sortBy == null || sortBy.isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(page, size);
        }
        org.springframework.data.domain.Sort.Direction direction;
        try {
            direction = org.springframework.data.domain.Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException e) {
            direction = org.springframework.data.domain.Sort.Direction.DESC;
        }
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(direction, sortBy);
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }
}
