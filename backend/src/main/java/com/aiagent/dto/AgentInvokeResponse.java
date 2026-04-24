package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Agent调用响应")
public class AgentInvokeResponse {
    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "异步任务ID")
    private String taskId;

    @Schema(description = "执行状态", example = "SUCCESS")
    private String status;

    @Schema(description = "输出结果")
    private Map<String, Object> outputs;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "执行时间(毫秒)")
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
