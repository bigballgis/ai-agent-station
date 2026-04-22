package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alert_rules")
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "threshold")
    private Double threshold;

    @Column(name = "comparison_operator")
    private String comparisonOperator = "gt"; // gt, lt, gte, lte, eq

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 300;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity = Severity.WARNING;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "notify_channels")
    private String notifyChannels = "email"; // email, webhook, sms

    @Column(name = "notify_targets")
    private String notifyTargets;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum AlertType {
        API_ERROR_RATE, API_RESPONSE_TIME, JVM_CPU, JVM_MEMORY,
        DB_CONNECTION_POOL, AGENT_EXECUTION_FAILURE, QUOTA_EXCEEDED
    }

    public enum Severity { INFO, WARNING, CRITICAL }
}
