package com.aiagent.service;

import com.aiagent.dto.AgentInvokeRequest;
import com.aiagent.dto.AgentInvokeResponse;
import com.aiagent.entity.ApiCallLog;
import com.aiagent.repository.ApiCallLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ApiCallLogService {

    private static final Logger log = LoggerFactory.getLogger(ApiCallLogService.class);

    private final ApiCallLogRepository apiCallLogRepository;
    private final ObjectMapper objectMapper;

    public ApiCallLogService(ApiCallLogRepository apiCallLogRepository, ObjectMapper objectMapper) {
        this.apiCallLogRepository = apiCallLogRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void logApiCall(String requestId, Long agentId, Long tenantId, Long userId, 
                          String requestMethod, String requestPath, String requestHeaders,
                          AgentInvokeRequest requestBody, AgentInvokeResponse response,
                          Integer responseStatus, String responseHeaders, ApiCallLog.ApiCallStatus status,
                          Integer executionTime, Boolean isAsync, String asyncTaskId) {
        try {
            ApiCallLog apiCallLog = new ApiCallLog();
            apiCallLog.setRequestId(requestId);
            apiCallLog.setAgentId(agentId);
            apiCallLog.setTenantId(tenantId);
            apiCallLog.setUserId(userId);
            apiCallLog.setRequestMethod(requestMethod);
            apiCallLog.setRequestPath(requestPath);
            apiCallLog.setRequestHeaders(requestHeaders);
            apiCallLog.setRequestBody(objectMapper.writeValueAsString(requestBody));
            apiCallLog.setResponseStatus(responseStatus);
            apiCallLog.setResponseHeaders(responseHeaders);
            apiCallLog.setResponseBody(objectMapper.writeValueAsString(response));
            apiCallLog.setStatus(status);
            apiCallLog.setErrorMessage(response.getErrorMessage());
            apiCallLog.setExecutionTime(executionTime);
            apiCallLog.setIsAsync(isAsync);
            apiCallLog.setAsyncTaskId(asyncTaskId);
            
            apiCallLogRepository.save(apiCallLog);
        } catch (JsonProcessingException e) {
            log.error("Error serializing API call log", e);
        }
    }
}
