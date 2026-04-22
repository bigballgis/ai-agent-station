package com.aiagent.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_approvals")
public class AgentApproval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_version_id", nullable = false)
    private Long agentVersionId;

    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    @Column(name = "approver_id")
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(length = 500)
    private String remark;

    @Column(name = "approval_remark", length = 500)
    private String approvalRemark;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", insertable = false, updatable = false)
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_version_id", insertable = false, updatable = false)
    private AgentVersion agentVersion;

    public AgentApproval() {
    }

    public AgentApproval(Long id, Long agentId, Long tenantId, Long agentVersionId, Long submitterId, Long approverId, ApprovalStatus status, String remark, String approvalRemark, LocalDateTime submittedAt, LocalDateTime approvedAt, Agent agent, Tenant tenant, AgentVersion agentVersion) {
        this.id = id;
        this.agentId = agentId;
        this.tenantId = tenantId;
        this.agentVersionId = agentVersionId;
        this.submitterId = submitterId;
        this.approverId = approverId;
        this.status = status;
        this.remark = remark;
        this.approvalRemark = approvalRemark;
        this.submittedAt = submittedAt;
        this.approvedAt = approvedAt;
        this.agent = agent;
        this.tenant = tenant;
        this.agentVersion = agentVersion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getAgentVersionId() {
        return agentVersionId;
    }

    public void setAgentVersionId(Long agentVersionId) {
        this.agentVersionId = agentVersionId;
    }

    public Long getSubmitterId() {
        return submitterId;
    }

    public void setSubmitterId(Long submitterId) {
        this.submitterId = submitterId;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getApprovalRemark() {
        return approvalRemark;
    }

    public void setApprovalRemark(String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public AgentVersion getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(AgentVersion agentVersion) {
        this.agentVersion = agentVersion;
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
