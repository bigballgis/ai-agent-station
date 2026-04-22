package com.aiagent.entity;

import jakarta.persistence.*;

import java.util.Map;

@Entity
@Table(name = "approval_chains")
public class ApprovalChain extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(columnDefinition = "jsonb")
    private Map<String, Object> steps;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChainStatus status = ChainStatus.ACTIVE;

    public ApprovalChain() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getSteps() {
        return steps;
    }

    public void setSteps(Map<String, Object> steps) {
        this.steps = steps;
    }

    public ChainStatus getStatus() {
        return status;
    }

    public void setStatus(ChainStatus status) {
        this.status = status;
    }

    public enum ChainStatus {
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }
}
