package com.aiagent.service;

import com.aiagent.entity.ApiCallAuditLog;
import com.aiagent.repository.ApiCallAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * API 调用审计日志服务
 *
 * 异步持久化 API 请求/响应日志到数据库，用于审计追溯。
 * 仅记录由 RequestResponseLoggingFilter 采集的网关层面日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiCallAuditLogService {

    private final ApiCallAuditLogRepository auditLogRepository;

    /** 响应体最大记录长度 */
    private static final int MAX_RESPONSE_BODY_LENGTH = 1000;

    /** 请求体最大记录长度 */
    private static final int MAX_REQUEST_BODY_LENGTH = 5000;

    /**
     * 异步保存审计日志
     */
    @Async
    public void saveAuditLog(ApiCallAuditLog auditLog) {
        try {
            // 截断过长的响应体
            if (auditLog.getResponseBody() != null
                    && auditLog.getResponseBody().length() > MAX_RESPONSE_BODY_LENGTH) {
                auditLog.setResponseBody(
                        auditLog.getResponseBody().substring(0, MAX_RESPONSE_BODY_LENGTH) + "...[truncated]");
            }

            // 截断过长的请求体
            if (auditLog.getRequestBody() != null
                    && auditLog.getRequestBody().length() > MAX_REQUEST_BODY_LENGTH) {
                auditLog.setRequestBody(
                        auditLog.getRequestBody().substring(0, MAX_REQUEST_BODY_LENGTH) + "...[truncated]");
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("保存API审计日志失败: {}", e.getMessage());
        }
    }
}
