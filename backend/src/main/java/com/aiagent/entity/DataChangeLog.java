package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_change_logs")
public class DataChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "record_id", nullable = false, length = 100)
    private String recordId;

    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType; // CREATE, UPDATE, DELETE

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "operator_ip", length = 50)
    private String operatorIp;

    @Column(name = "operated_at")
    private LocalDateTime operatedAt;

    @Column(name = "tenant_id")
    private Long tenantId;

    @PrePersist
    protected void onCreate() {
        if (this.operatedAt == null) {
            this.operatedAt = LocalDateTime.now();
        }
    }
}
