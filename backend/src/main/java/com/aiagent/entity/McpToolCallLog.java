package com.aiagent.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mcp_tool_call_logs")
public class McpToolCallLog {

    /**
     * MCP 调用失败错误分类枚举
     */
    public enum ErrorCategory {
        /** 工具未在超时时间内响应 */
        TIMEOUT,
        /** 认证/鉴权失败 */
        AUTH_FAILURE,
        /** 工具触发了速率限制 */
        RATE_LIMITED,
        /** 请求格式不正确或参数校验失败 */
        INVALID_REQUEST,
        /** 工具内部错误 */
        INTERNAL_ERROR,
        /** 无错误（成功调用） */
        NONE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mcp_tool_id", nullable = false)
    private Long mcpToolId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "api_call_log_id")
    private Long apiCallLogId;

    @Column(name = "request_params", columnDefinition = "text")
    private String requestParams;

    @Column(name = "response_result", columnDefinition = "text")
    private String responseResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiCallLog.ApiCallStatus status = ApiCallLog.ApiCallStatus.SUCCESS;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * 错误分类: TIMEOUT / AUTH_FAILURE / RATE_LIMITED / INVALID_REQUEST / INTERNAL_ERROR / NONE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", length = 20)
    private ErrorCategory errorCategory = ErrorCategory.NONE;

    @Column(name = "execution_time")
    private Integer executionTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mcp_tool_id", insertable = false, updatable = false)
    private McpTool mcpTool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_call_log_id", insertable = false, updatable = false)
    private ApiCallLog apiCallLog;

    public McpToolCallLog() {
    }

    public McpToolCallLog(Long id, Long mcpToolId, Long tenantId, Long apiCallLogId, String requestParams, String responseResult, ApiCallLog.ApiCallStatus status, String errorMessage, Integer executionTime, LocalDateTime createdAt, McpTool mcpTool, Tenant tenant, ApiCallLog apiCallLog) {
        this.id = id;
        this.mcpToolId = mcpToolId;
        this.tenantId = tenantId;
        this.apiCallLogId = apiCallLogId;
        this.requestParams = requestParams;
        this.responseResult = responseResult;
        this.status = status;
        this.errorMessage = errorMessage;
        this.executionTime = executionTime;
        this.createdAt = createdAt;
        this.mcpTool = mcpTool;
        this.tenant = tenant;
        this.apiCallLog = apiCallLog;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMcpToolId() {
        return mcpToolId;
    }

    public void setMcpToolId(Long mcpToolId) {
        this.mcpToolId = mcpToolId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getApiCallLogId() {
        return apiCallLogId;
    }

    public void setApiCallLogId(Long apiCallLogId) {
        this.apiCallLogId = apiCallLogId;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResponseResult() {
        return responseResult;
    }

    public void setResponseResult(String responseResult) {
        this.responseResult = responseResult;
    }

    public ApiCallLog.ApiCallStatus getStatus() {
        return status;
    }

    public void setStatus(ApiCallLog.ApiCallStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Integer executionTime) {
        this.executionTime = executionTime;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public McpTool getMcpTool() {
        return mcpTool;
    }

    public void setMcpTool(McpTool mcpTool) {
        this.mcpTool = mcpTool;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public ApiCallLog getApiCallLog() {
        return apiCallLog;
    }

    public void setApiCallLog(ApiCallLog apiCallLog) {
        this.apiCallLog = apiCallLog;
    }

    public ErrorCategory getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(ErrorCategory errorCategory) {
        this.errorCategory = errorCategory;
    }
}
