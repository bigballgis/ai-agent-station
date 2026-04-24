package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "Agent调用请求")
public class AgentInvokeRequest {
    @NotEmpty(message = "{error.validation.inputs_required}")
    @Schema(description = "输入参数", requiredMode = Schema.RequiredMode.REQUIRED, example = "{\"message\": \"hello\"}")
    private Map<String, Object> inputs;

    @Schema(description = "上下文参数")
    private Map<String, Object> context;

    @Schema(description = "是否异步执行", example = "false")
    private Boolean async = false;

    @Size(max = 500, message = "{error.validation.callback_url_too_long}")
    @Schema(description = "异步回调URL", example = "https://example.com/callback")
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
