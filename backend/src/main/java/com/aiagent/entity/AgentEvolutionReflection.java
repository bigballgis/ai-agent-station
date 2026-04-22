package com.aiagent.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "agent_evolution_reflections")
public class AgentEvolutionReflection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "evaluation_type", nullable = false, length = 50)
    private String evaluationType;

    @Column(name = "evaluation_metrics", columnDefinition = "jsonb")
    private String evaluationMetrics;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore;

    @Column(name = "accuracy_score", precision = 5, scale = 2)
    private BigDecimal accuracyScore;

    @Column(name = "efficiency_score", precision = 5, scale = 2)
    private BigDecimal efficiencyScore;

    @Column(name = "user_satisfaction_score", precision = 5, scale = 2)
    private BigDecimal userSatisfactionScore;

    @Column(name = "strengths", columnDefinition = "text[]")
    private List<String> strengths;

    @Column(name = "weaknesses", columnDefinition = "text[]")
    private List<String> weaknesses;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    public AgentEvolutionReflection() {
    }

    public AgentEvolutionReflection(Long id, Long tenantId, Long agentId, String evaluationType, String evaluationMetrics, BigDecimal performanceScore, BigDecimal accuracyScore, BigDecimal efficiencyScore, BigDecimal userSatisfactionScore, List<String> strengths, List<String> weaknesses, String summary, Integer status, Long createdBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.agentId = agentId;
        this.evaluationType = evaluationType;
        this.evaluationMetrics = evaluationMetrics;
        this.performanceScore = performanceScore;
        this.accuracyScore = accuracyScore;
        this.efficiencyScore = efficiencyScore;
        this.userSatisfactionScore = userSatisfactionScore;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.summary = summary;
        this.status = status;
        this.createdBy = createdBy;
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

    public String getEvaluationType() {
        return evaluationType;
    }

    public void setEvaluationType(String evaluationType) {
        this.evaluationType = evaluationType;
    }

    public String getEvaluationMetrics() {
        return evaluationMetrics;
    }

    public void setEvaluationMetrics(String evaluationMetrics) {
        this.evaluationMetrics = evaluationMetrics;
    }

    public BigDecimal getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(BigDecimal performanceScore) {
        this.performanceScore = performanceScore;
    }

    public BigDecimal getAccuracyScore() {
        return accuracyScore;
    }

    public void setAccuracyScore(BigDecimal accuracyScore) {
        this.accuracyScore = accuracyScore;
    }

    public BigDecimal getEfficiencyScore() {
        return efficiencyScore;
    }

    public void setEfficiencyScore(BigDecimal efficiencyScore) {
        this.efficiencyScore = efficiencyScore;
    }

    public BigDecimal getUserSatisfactionScore() {
        return userSatisfactionScore;
    }

    public void setUserSatisfactionScore(BigDecimal userSatisfactionScore) {
        this.userSatisfactionScore = userSatisfactionScore;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
