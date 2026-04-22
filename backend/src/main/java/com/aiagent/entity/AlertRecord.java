package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alert_records")
public class AlertRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "alert_type")
    private String alertType;

    @Column(name = "severity")
    private String severity;

    @Column(name = "message")
    private String message;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "threshold")
    private Double threshold;

    @Column(name = "status")
    private String status = "firing"; // firing, resolved

    @Column(name = "fired_at")
    private LocalDateTime firedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "tenant_id")
    private Long tenantId;

    @PrePersist
    protected void onCreate() { firedAt = LocalDateTime.now(); }
}
