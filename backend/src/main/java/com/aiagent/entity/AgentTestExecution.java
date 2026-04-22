package com.aiagent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "agent_test_executions")
public class AgentTestExecution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "test_case_id", nullable = false)
    private Long testCaseId;

    @Column(name = "execution_type", nullable = false, length = 20, columnDefinition = "varchar(20) default 'manual'")
    private String executionType;

    @Column(name = "executor_id", nullable = false)
    private Long executorId;

    @Column(name = "status", nullable = false, columnDefinition = "smallint default 0")
    private Integer status;

    @Column(name = "start_time", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "execution_time")
    private Integer executionTime;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AgentTestCase testCase;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AgentTestResult> results;

    // Getters and Setters
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

    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
    }

    public Long getExecutorId() {
        return executorId;
    }

    public void setExecutorId(Long executorId) {
        this.executorId = executorId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Integer executionTime) {
        this.executionTime = executionTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public AgentTestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(AgentTestCase testCase) {
        this.testCase = testCase;
    }

    public List<AgentTestResult> getResults() {
        return results;
    }

    public void setResults(List<AgentTestResult> results) {
        this.results = results;
    }
}