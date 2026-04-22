package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dict_items")
public class DictItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dict_type", nullable = false, length = 100)
    private String dictType;

    @Column(name = "dict_label", nullable = false, length = 200)
    private String dictLabel;

    @Column(name = "dict_value", nullable = false, length = 200)
    private String dictValue;

    @Column(name = "dict_sort")
    private Integer dictSort = 0;

    @Column(name = "css_class", length = 100)
    private String cssClass;

    @Column(name = "list_class", length = 100)
    private String listClass;

    @Column(name = "is_default", length = 10)
    private String isDefault = "N";

    @Column(name = "status", length = 10)
    private String status = "active";

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
