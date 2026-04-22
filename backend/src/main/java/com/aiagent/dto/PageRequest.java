package com.aiagent.dto;

import lombok.Data;

@Data
public class PageRequest {
    private int page = 0;
    private int size = 20;
    private String sortBy;
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
