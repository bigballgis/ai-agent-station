package com.aiagent.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mcp_tools")
public class McpTool extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "tool_code", nullable = false, length = 100)
    private String toolCode;

    @Column(name = "tool_type", nullable = false, length = 50)
    private String toolType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "endpoint_url", length = 500)
    private String endpointUrl;

    @Column(columnDefinition = "text")
    private String config;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    // ==================== 健康检查字段 ====================

    /**
     * 健康状态: HEALTHY / DEGRADED / UNHEALTHY / UNKNOWN
     */
    @Column(name = "health_status", length = 20)
    private String healthStatus = "UNKNOWN";

    /**
     * 最近一次健康检查时间
     */
    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    /**
     * 连续失败次数（用于自动禁用判定）
     */
    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    /**
     * 平均响应时间（毫秒）
     */
    @Column(name = "avg_response_time")
    private Long avgResponseTime = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    public McpTool() {
    }

    public McpTool(Long id, Long tenantId, String toolName, String toolCode, String toolType, String description, String endpointUrl, String config, Boolean isActive, Long createdBy, Tenant tenant) {
        this.id = id;
        this.tenantId = tenantId;
        this.toolName = toolName;
        this.toolCode = toolCode;
        this.toolType = toolType;
        this.description = description;
        this.endpointUrl = endpointUrl;
        this.config = config;
        this.isActive = isActive;
        this.createdBy = createdBy;
        this.tenant = tenant;
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

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    // 添加getName方法，因为McpToolGateway.java中使用了tool.getName()
    public String getName() {
        return toolName;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }

    public void setLastHealthCheck(LocalDateTime lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }

    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public Long getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(Long avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }
}
