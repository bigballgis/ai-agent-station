package com.aiagent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "agent_test_cases")
public class AgentTestCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Column(name = "test_code", nullable = false, length = 50)
    private String testCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "test_type", nullable = false, length = 20, columnDefinition = "varchar(20) default 'unit'")
    private String testType;

    @Column(name = "input_params", nullable = false, columnDefinition = "jsonb default '{}'::jsonb")
    private String inputParams;

    @Column(name = "expected_output", nullable = false, columnDefinition = "jsonb default '{}'::jsonb")
    private String expectedOutput;

    @Column(name = "status", nullable = false, columnDefinition = "smallint default 1")
    private Integer status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    // 关联关系
    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AgentTestExecution> executions;

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

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getInputParams() {
        return inputParams;
    }

    public void setInputParams(String inputParams) {
        this.inputParams = inputParams;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public List<AgentTestExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<AgentTestExecution> executions) {
        this.executions = executions;
    }
}