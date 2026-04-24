package com.aiagent.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_call_logs")
public class ApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_interface_id")
    private Long apiInterfaceId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod;

    @Column(name = "request_path", nullable = false, length = 500)
    private String requestPath;

    @Column(name = "request_headers", columnDefinition = "text")
    private String requestHeaders;

    @Column(name = "request_params", columnDefinition = "text")
    private String requestParams;

    @Column(name = "request_body", columnDefinition = "text")
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_headers", columnDefinition = "text")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiCallStatus status = ApiCallStatus.SUCCESS;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "execution_time")
    private Integer executionTime;

    @Column(name = "is_async", nullable = false)
    private Boolean isAsync = false;

    @Column(name = "async_task_id", length = 100)
    private String asyncTaskId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_interface_id", insertable = false, updatable = false)
    private ApiInterface apiInterface;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", insertable = false, updatable = false)
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    public ApiCallLog() {
    }

    public ApiCallLog(Long id, Long apiInterfaceId, Long agentId, Long tenantId, Long userId, String clientIp, String requestId, String requestMethod, String requestPath, String requestHeaders, String requestParams, String requestBody, Integer responseStatus, String responseHeaders, String responseBody, ApiCallStatus status, String errorMessage, Integer executionTime, Boolean isAsync, String asyncTaskId, LocalDateTime createdAt, ApiInterface apiInterface, Agent agent, Tenant tenant) {
        this.id = id;
        this.apiInterfaceId = apiInterfaceId;
        this.agentId = agentId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.clientIp = clientIp;
        this.requestId = requestId;
        this.requestMethod = requestMethod;
        this.requestPath = requestPath;
        this.requestHeaders = requestHeaders;
        this.requestParams = requestParams;
        this.requestBody = requestBody;
        this.responseStatus = responseStatus;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.status = status;
        this.errorMessage = errorMessage;
        this.executionTime = executionTime;
        this.isAsync = isAsync;
        this.asyncTaskId = asyncTaskId;
        this.createdAt = createdAt;
        this.apiInterface = apiInterface;
        this.agent = agent;
        this.tenant = tenant;
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

    public Long getApiInterfaceId() {
        return apiInterfaceId;
    }

    public void setApiInterfaceId(Long apiInterfaceId) {
        this.apiInterfaceId = apiInterfaceId;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public ApiCallStatus getStatus() {
        return status;
    }

    public void setStatus(ApiCallStatus status) {
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

    public Boolean getIsAsync() {
        return isAsync;
    }

    public void setIsAsync(Boolean isAsync) {
        this.isAsync = isAsync;
    }

    public String getAsyncTaskId() {
        return asyncTaskId;
    }

    public void setAsyncTaskId(String asyncTaskId) {
        this.asyncTaskId = asyncTaskId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ApiInterface getApiInterface() {
        return apiInterface;
    }

    public void setApiInterface(ApiInterface apiInterface) {
        this.apiInterface = apiInterface;
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

    public enum ApiCallStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        RATE_LIMITED,
        UNAUTHORIZED
    }
}
