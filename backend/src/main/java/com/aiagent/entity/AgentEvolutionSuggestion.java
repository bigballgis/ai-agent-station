package com.aiagent.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_evolution_suggestions")
public class AgentEvolutionSuggestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "reflection_id")
    private Long reflectionId;

    @Column(name = "suggestion_type", nullable = false, length = 50)
    private String suggestionType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "content", columnDefinition = "jsonb")
    private String content;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "implementation_status", nullable = false, length = 20)
    private String implementationStatus;

    @Column(name = "expected_impact", precision = 5, scale = 2)
    private BigDecimal expectedImpact;

    @Column(name = "actual_impact", precision = 5, scale = 2)
    private BigDecimal actualImpact;

    @Column(name = "implemented_by")
    private Long implementedBy;

    @Column(name = "implemented_at")
    private LocalDateTime implementedAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    public AgentEvolutionSuggestion() {
    }

    public AgentEvolutionSuggestion(Long id, Long tenantId, Long agentId, Long reflectionId, String suggestionType, String title, String description, String content, Integer priority, String status, String implementationStatus, BigDecimal expectedImpact, BigDecimal actualImpact, Long implementedBy, LocalDateTime implementedAt, Long createdBy, Long updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.agentId = agentId;
        this.reflectionId = reflectionId;
        this.suggestionType = suggestionType;
        this.title = title;
        this.description = description;
        this.content = content;
        this.priority = priority;
        this.status = status;
        this.implementationStatus = implementationStatus;
        this.expectedImpact = expectedImpact;
        this.actualImpact = actualImpact;
        this.implementedBy = implementedBy;
        this.implementedAt = implementedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getReflectionId() {
        return reflectionId;
    }

    public void setReflectionId(Long reflectionId) {
        this.reflectionId = reflectionId;
    }

    public String getSuggestionType() {
        return suggestionType;
    }

    public void setSuggestionType(String suggestionType) {
        this.suggestionType = suggestionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImplementationStatus() {
        return implementationStatus;
    }

    public void setImplementationStatus(String implementationStatus) {
        this.implementationStatus = implementationStatus;
    }

    public BigDecimal getExpectedImpact() {
        return expectedImpact;
    }

    public void setExpectedImpact(BigDecimal expectedImpact) {
        this.expectedImpact = expectedImpact;
    }

    public BigDecimal getActualImpact() {
        return actualImpact;
    }

    public void setActualImpact(BigDecimal actualImpact) {
        this.actualImpact = actualImpact;
    }

    public Long getImplementedBy() {
        return implementedBy;
    }

    public void setImplementedBy(Long implementedBy) {
        this.implementedBy = implementedBy;
    }

    public LocalDateTime getImplementedAt() {
        return implementedAt;
    }

    public void setImplementedAt(LocalDateTime implementedAt) {
        this.implementedAt = implementedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
