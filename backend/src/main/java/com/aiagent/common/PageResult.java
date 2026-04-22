package com.aiagent.common;

import java.io.Serializable;
import java.util.List;

public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long total;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    public static <T> PageResult<T> of(Long total, List<T> records) {
        return new PageResult<>(total, records);
    }

    public static <T> PageResult<T> from(org.springframework.data.domain.Page<T> page) {
        return new PageResult<>(page.getTotalElements(), page.getContent());
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
