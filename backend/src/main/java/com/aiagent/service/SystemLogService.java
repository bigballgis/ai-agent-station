package com.aiagent.service;

import com.aiagent.common.PageResult;
import com.aiagent.entity.SystemLog;
import com.aiagent.repository.SystemLogRepository;
import com.aiagent.tenant.TenantContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SystemLogService {

    private static final Logger log = LoggerFactory.getLogger(SystemLogService.class);
    private final SystemLogRepository systemLogRepository;
    private final ObjectMapper objectMapper;

    public SystemLogService(SystemLogRepository systemLogRepository, ObjectMapper objectMapper) {
        this.systemLogRepository = systemLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveLog(SystemLog systemLog) {
        try {
            systemLogRepository.save(systemLog);
        } catch (Exception e) {
            log.error("保存日志失败", e);
        }
    }

    public PageResult<SystemLog> getLogs(int page, int size) {
        Long tenantId = TenantContextHolder.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<SystemLog> logPage;
        if (tenantId != null) {
            logPage = systemLogRepository.findByTenantId(tenantId, pageable);
        } else {
            logPage = systemLogRepository.findAll(pageable);
        }
        
        return PageResult.of(logPage.getTotalElements(), logPage.getContent());
    }

    public PageResult<SystemLog> getLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Long tenantId = TenantContextHolder.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<SystemLog> logPage;
        if (tenantId != null) {
            logPage = systemLogRepository.findByTenantIdAndCreatedAtBetween(tenantId, startTime, endTime, pageable);
        } else {
            logPage = systemLogRepository.findAll(pageable);
        }
        
        return PageResult.of(logPage.getTotalElements(), logPage.getContent());
    }

    public PageResult<SystemLog> getLogsByModule(String module, int page, int size) {
        Long tenantId = TenantContextHolder.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<SystemLog> logPage;
        if (tenantId != null) {
            logPage = systemLogRepository.findByTenantIdAndModule(tenantId, module, pageable);
        } else {
            logPage = systemLogRepository.findAll(pageable);
        }
        
        return PageResult.of(logPage.getTotalElements(), logPage.getContent());
    }

    public SystemLog getLogById(Long id) {
        return systemLogRepository.findById(id).orElse(null);
    }

    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
