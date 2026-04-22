package com.aiagent.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "agent_evolution_experiences", uniqueConstraints = {
    @UniqueConstraint(name = "uk_experience_code", columnNames = {"experience_code", "tenant_id"})
})
public class AgentEvolutionExperience extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "experience_type", nullable = false, length = 50)
    private String experienceType;

    @Column(name = "experience_code", nullable = false, length = 50)
    private String experienceCode;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "content", columnDefinition = "jsonb")
    private String content;

    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount;

    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    public AgentEvolutionExperience() {
    }

    public AgentEvolutionExperience(Long id, Long tenantId, Long agentId, String experienceType, String experienceCode, String title, String description, String content, List<String> tags, Integer usageCount, BigDecimal effectivenessScore, Integer status, Long createdBy, Long updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.agentId = agentId;
        this.experienceType = experienceType;
        this.experienceCode = experienceCode;
        this.title = title;
        this.description = description;
        this.content = content;
        this.tags = tags;
        this.usageCount = usageCount;
        this.effectivenessScore = effectivenessScore;
        this.status = status;
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

    public String getExperienceType() {
        return experienceType;
    }

    public void setExperienceType(String experienceType) {
        this.experienceType = experienceType;
    }

    public String getExperienceCode() {
        return experienceCode;
    }

    public void setExperienceCode(String experienceCode) {
        this.experienceCode = experienceCode;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public BigDecimal getEffectivenessScore() {
        return effectivenessScore;
    }

    public void setEffectivenessScore(BigDecimal effectivenessScore) {
        this.effectivenessScore = effectivenessScore;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
