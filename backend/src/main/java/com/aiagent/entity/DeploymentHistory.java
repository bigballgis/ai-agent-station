package com.aiagent.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deployment_history")
public class DeploymentHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_version_id", nullable = false)
    private Long agentVersionId;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "deployer_id", nullable = false)
    private Long deployerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStatus status = DeploymentStatus.PENDING;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "is_canary", nullable = false)
    private Boolean isCanary = false;

    @Column(name = "canary_percentage")
    private Integer canaryPercentage = 0;

    @Column(name = "rollback_from_id")
    private Long rollbackFromId;

    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @Column(name = "rollback_at")
    private LocalDateTime rollbackAt;

    @Column(length = 500)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", insertable = false, updatable = false)
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_version_id", insertable = false, updatable = false)
    private AgentVersion agentVersion;

    public DeploymentHistory() {
    }

    public DeploymentHistory(Long id, Long agentId, Long tenantId, Long agentVersionId, Long approverId, Long deployerId, DeploymentStatus status, String version, Boolean isCanary, Integer canaryPercentage, Long rollbackFromId, LocalDateTime deployedAt, LocalDateTime rollbackAt, String remark, Agent agent, Tenant tenant, AgentVersion agentVersion) {
        this.id = id;
        this.agentId = agentId;
        this.tenantId = tenantId;
        this.agentVersionId = agentVersionId;
        this.approverId = approverId;
        this.deployerId = deployerId;
        this.status = status;
        this.version = version;
        this.isCanary = isCanary;
        this.canaryPercentage = canaryPercentage;
        this.rollbackFromId = rollbackFromId;
        this.deployedAt = deployedAt;
        this.rollbackAt = rollbackAt;
        this.remark = remark;
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

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public Long getDeployerId() {
        return deployerId;
    }

    public void setDeployerId(Long deployerId) {
        this.deployerId = deployerId;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(DeploymentStatus status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getIsCanary() {
        return isCanary;
    }

    public void setIsCanary(Boolean isCanary) {
        this.isCanary = isCanary;
    }

    public Integer getCanaryPercentage() {
        return canaryPercentage;
    }

    public void setCanaryPercentage(Integer canaryPercentage) {
        this.canaryPercentage = canaryPercentage;
    }

    public Long getRollbackFromId() {
        return rollbackFromId;
    }

    public void setRollbackFromId(Long rollbackFromId) {
        this.rollbackFromId = rollbackFromId;
    }

    public LocalDateTime getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(LocalDateTime deployedAt) {
        this.deployedAt = deployedAt;
    }

    public LocalDateTime getRollbackAt() {
        return rollbackAt;
    }

    public void setRollbackAt(LocalDateTime rollbackAt) {
        this.rollbackAt = rollbackAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public enum DeploymentStatus {
        PENDING,
        DEPLOYING,
        SUCCESS,
        FAILED,
        ROLLED_BACK
    }
}
