package com.aiagent.dto;

import java.util.Map;

public class AgentInvokeRequest {
    private Map<String, Object> inputs;
    private Map<String, Object> context;
    private Boolean async = false;
    private String callbackUrl;

    public AgentInvokeRequest() {
    }

    public AgentInvokeRequest(Map<String, Object> inputs, Map<String, Object> context, Boolean async, String callbackUrl) {
        this.inputs = inputs;
        this.context = context;
        this.async = async;
        this.callbackUrl = callbackUrl;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}
