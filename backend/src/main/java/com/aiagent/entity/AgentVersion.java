package com.aiagent.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "agent_versions")
public class AgentVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "change_log", length = 500)
    private String changeLog;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "semantic_version", length = 50)
    private String semanticVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", insertable = false, updatable = false)
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    public AgentVersion() {
    }

    public AgentVersion(Long id, Long agentId, Long tenantId, Integer versionNumber, Map<String, Object> config, String changeLog, Long createdBy, String semanticVersion, Agent agent, Tenant tenant) {
        this.id = id;
        this.agentId = agentId;
        this.tenantId = tenantId;
        this.versionNumber = versionNumber;
        this.config = config;
        this.changeLog = changeLog;
        this.createdBy = createdBy;
        this.semanticVersion = semanticVersion;
        this.agent = agent;
        this.tenant = tenant;
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

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getSemanticVersion() {
        return semanticVersion;
    }

    public void setSemanticVersion(String semanticVersion) {
        this.semanticVersion = semanticVersion;
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
}
