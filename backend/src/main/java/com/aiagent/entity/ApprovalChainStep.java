package com.aiagent.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_chain_steps")
public class ApprovalChainStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chain_id", nullable = false)
    private Long chainId;

    @Column(name = "approval_id", nullable = false)
    private Long approvalId;

    @Column(name = "step_level", nullable = false)
    private Integer stepLevel;

    @Column(name = "step_name", length = 200)
    private String stepName;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "approver_name", length = 100)
    private String approverName;

    @Column(name = "approver_type", length = 50)
    private String approverType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepStatus status = StepStatus.PENDING;

    @Column(length = 1000)
    private String comment;

    @Column(name = "acted_at")
    private LocalDateTime actedAt;

    public ApprovalChainStep() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChainId() {
        return chainId;
    }

    public void setChainId(Long chainId) {
        this.chainId = chainId;
    }

    public Long getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(Long approvalId) {
        this.approvalId = approvalId;
    }

    public Integer getStepLevel() {
        return stepLevel;
    }

    public void setStepLevel(Integer stepLevel) {
        this.stepLevel = stepLevel;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApproverType() {
        return approverType;
    }

    public void setApproverType(String approverType) {
        this.approverType = approverType;
    }

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getActedAt() {
        return actedAt;
    }

    public void setActedAt(LocalDateTime actedAt) {
        this.actedAt = actedAt;
    }

    public enum StepStatus {
        PENDING,
        APPROVED,
        REJECTED,
        SKIPPED
    }
}
