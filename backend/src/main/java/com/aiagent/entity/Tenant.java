package com.aiagent.entity;

import com.aiagent.annotation.Sensitive;
import com.aiagent.annotation.SensitiveType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "schema_name", unique = true, length = 100)
    private String schemaName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @JsonIgnore
    @Sensitive(type = SensitiveType.PARTIAL, maskPrefix = 4, maskSuffix = 4)
    @Column(name = "api_key", unique = true, length = 100)
    private String apiKey;

    @JsonIgnore
    @Sensitive(type = SensitiveType.PARTIAL, maskPrefix = 4, maskSuffix = 4)
    @Column(name = "api_secret", length = 100)
    private String apiSecret;

    @Column(name = "max_agents", nullable = false, columnDefinition = "int default 100")
    private Integer maxAgents = 100;

    @Column(name = "max_api_calls_per_day", nullable = false, columnDefinition = "bigint default 10000")
    private Long maxApiCallsPerDay = 10000L;

    @Column(name = "max_tokens_per_day", nullable = false, columnDefinition = "bigint default 1000000")
    private Long maxTokensPerDay = 1000000L;

    @Column(name = "max_mcp_calls_per_day", nullable = false, columnDefinition = "bigint default 5000")
    private Long maxMcpCallsPerDay = 5000L;

    @Column(name = "max_storage_mb", nullable = false, columnDefinition = "bigint default 1024")
    private Long maxStorageMb = 1024L;

    @Column(name = "max_workflows", nullable = false, columnDefinition = "int default 50")
    private Integer maxWorkflows = 50;

    @Column(name = "used_agents", nullable = false, columnDefinition = "int default 0")
    private Integer usedAgents = 0;

    @Column(name = "used_api_calls_today", nullable = false, columnDefinition = "bigint default 0")
    private Long usedApiCallsToday = 0L;

    @Column(name = "used_tokens_today", nullable = false, columnDefinition = "bigint default 0")
    private Long usedTokensToday = 0L;

    @Column(name = "used_workflows", nullable = false, columnDefinition = "int default 0")
    private Integer usedWorkflows = 0;

    public Tenant() {
    }

    public Tenant(Long id, String name, String description, String schemaName, Boolean isActive, String apiKey, String apiSecret) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.schemaName = schemaName;
        this.isActive = isActive;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
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

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public Integer getMaxAgents() {
        return maxAgents;
    }

    public void setMaxAgents(Integer maxAgents) {
        this.maxAgents = maxAgents;
    }

    public Long getMaxApiCallsPerDay() {
        return maxApiCallsPerDay;
    }

    public void setMaxApiCallsPerDay(Long maxApiCallsPerDay) {
        this.maxApiCallsPerDay = maxApiCallsPerDay;
    }

    public Long getMaxTokensPerDay() {
        return maxTokensPerDay;
    }

    public void setMaxTokensPerDay(Long maxTokensPerDay) {
        this.maxTokensPerDay = maxTokensPerDay;
    }

    public Long getMaxMcpCallsPerDay() {
        return maxMcpCallsPerDay;
    }

    public void setMaxMcpCallsPerDay(Long maxMcpCallsPerDay) {
        this.maxMcpCallsPerDay = maxMcpCallsPerDay;
    }

    public Long getMaxStorageMb() {
        return maxStorageMb;
    }

    public void setMaxStorageMb(Long maxStorageMb) {
        this.maxStorageMb = maxStorageMb;
    }

    public Integer getUsedAgents() {
        return usedAgents;
    }

    public void setUsedAgents(Integer usedAgents) {
        this.usedAgents = usedAgents;
    }

    public Long getUsedApiCallsToday() {
        return usedApiCallsToday;
    }

    public void setUsedApiCallsToday(Long usedApiCallsToday) {
        this.usedApiCallsToday = usedApiCallsToday;
    }

    public Long getUsedTokensToday() {
        return usedTokensToday;
    }

    public void setUsedTokensToday(Long usedTokensToday) {
        this.usedTokensToday = usedTokensToday;
    }

    public Integer getMaxWorkflows() {
        return maxWorkflows;
    }

    public void setMaxWorkflows(Integer maxWorkflows) {
        this.maxWorkflows = maxWorkflows;
    }

    public Integer getUsedWorkflows() {
        return usedWorkflows;
    }

    public void setUsedWorkflows(Integer usedWorkflows) {
        this.usedWorkflows = usedWorkflows;
    }
}
