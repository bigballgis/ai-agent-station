package com.aiagent.common;

import java.io.Serializable;
import java.util.List;

public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long total;
    private List<T> records;
    private int page;
    private int size;
    private int totalPages;

    public PageResult() {
    }

    public PageResult(Long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    public PageResult(Long total, List<T> records, int page, int size) {
        this.total = total;
        this.records = records;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    public static <T> PageResult<T> of(Long total, List<T> records) {
        return new PageResult<>(total, records);
    }

    public static <T> PageResult<T> of(Long total, List<T> records, int page, int size) {
        return new PageResult<>(total, records, page, size);
    }

    public static <T> PageResult<T> from(org.springframework.data.domain.Page<T> page) {
        return new PageResult<>(
                page.getTotalElements(),
                page.getContent(),
                page.getNumber(),
                page.getSize()
        );
    }

    /**
     * 对全量列表进行内存分页。
     * TODO: 后续应改为数据库层面分页，避免内存中处理大量数据
     */
    public static <T> PageResult<T> paginate(List<T> allRecords, int page, int size) {
        int total = allRecords.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<T> pageRecords = allRecords.subList(fromIndex, toIndex);
        return new PageResult<>((long) total, pageRecords, page, size);
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
