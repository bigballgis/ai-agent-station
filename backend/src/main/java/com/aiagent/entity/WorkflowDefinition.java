package com.aiagent.entity;

import jakarta.persistence.*;

import java.util.Map;

@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Version
    @Column(name = "optimistic_version")
    private Long optimisticVersion;

    @Column(nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @Column(columnDefinition = "jsonb")
    private Map<String, Object> nodes;

    @Column(columnDefinition = "jsonb")
    private Map<String, Object> edges;

    @Column(columnDefinition = "jsonb")
    private Map<String, Object> triggers;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * 基础定义ID，用于关联同一工作流的不同版本。
     * 首次创建时等于自身ID，创建新版本时继承自原定义。
     */
    @Column(name = "base_definition_id")
    private Long baseDefinitionId;

    public WorkflowDefinition() {
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public Map<String, Object> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, Object> nodes) {
        this.nodes = nodes;
    }

    public Map<String, Object> getEdges() {
        return edges;
    }

    public void setEdges(Map<String, Object> edges) {
        this.edges = edges;
    }

    public Map<String, Object> getTriggers() {
        return triggers;
    }

    public void setTriggers(Map<String, Object> triggers) {
        this.triggers = triggers;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getBaseDefinitionId() {
        return baseDefinitionId;
    }

    public void setBaseDefinitionId(Long baseDefinitionId) {
        this.baseDefinitionId = baseDefinitionId;
    }

    public enum WorkflowStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
}
