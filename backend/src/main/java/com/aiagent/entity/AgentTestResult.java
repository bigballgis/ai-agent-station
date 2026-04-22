package com.aiagent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_test_results")
public class AgentTestResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "test_case_id", nullable = false)
    private Long testCaseId;

    @Column(name = "actual_output", nullable = false, columnDefinition = "jsonb default '{}'::jsonb")
    private String actualOutput;

    @Column(name = "expected_output", nullable = false, columnDefinition = "jsonb default '{}'::jsonb")
    private String expectedOutput;

    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20) default 'pending'")
    private String status;

    @Column(name = "comparison_result", columnDefinition = "jsonb default '{}'::jsonb")
    private String comparisonResult;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AgentTestExecution execution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AgentTestCase testCase;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
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

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComparisonResult() {
        return comparisonResult;
    }

    public void setComparisonResult(String comparisonResult) {
        this.comparisonResult = comparisonResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public AgentTestExecution getExecution() {
        return execution;
    }

    public void setExecution(AgentTestExecution execution) {
        this.execution = execution;
    }

    public AgentTestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(AgentTestCase testCase) {
        this.testCase = testCase;
    }
}