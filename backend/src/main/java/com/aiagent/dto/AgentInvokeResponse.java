package com.aiagent.dto;

import java.util.Map;

public class AgentInvokeResponse {
    private String requestId;
    private String taskId;
    private String status;
    private Map<String, Object> outputs;
    private String errorMessage;
    private Integer executionTime;

    public AgentInvokeResponse() {
    }

    public AgentInvokeResponse(String requestId, String taskId, String status, Map<String, Object> outputs, String errorMessage, Integer executionTime) {
        this.requestId = requestId;
        this.taskId = taskId;
        this.status = status;
        this.outputs = outputs;
        this.errorMessage = errorMessage;
        this.executionTime = executionTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
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
}
