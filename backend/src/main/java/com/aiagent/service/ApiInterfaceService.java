package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.ApiInterface;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.ApiInterfaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiInterfaceService {

    private final ApiInterfaceRepository apiInterfaceRepository;

    @Transactional
    public ApiInterface create(ApiInterface apiInterface) {
        log.info("Creating API interface for agentId={}, tenantId={}", apiInterface.getAgentId(), apiInterface.getTenantId());
        return apiInterfaceRepository.save(apiInterface);
    }

    @Transactional
    public ApiInterface update(Long id, Long tenantId, ApiInterface apiInterface) {
        ApiInterface existing = apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));

        if (apiInterface.getAgentId() != null) {
            existing.setAgentId(apiInterface.getAgentId());
        }
        if (apiInterface.getVersionId() != null) {
            existing.setVersionId(apiInterface.getVersionId());
        }
        if (apiInterface.getPath() != null) {
            existing.setPath(apiInterface.getPath());
        }
        if (apiInterface.getMethod() != null) {
            existing.setMethod(apiInterface.getMethod());
        }
        if (apiInterface.getDescription() != null) {
            existing.setDescription(apiInterface.getDescription());
        }
        if (apiInterface.getIsActive() != null) {
            existing.setIsActive(apiInterface.getIsActive());
        }

        log.info("Updated API interface id={}, tenantId={}", id, tenantId);
        return apiInterfaceRepository.save(existing);
    }

    @Transactional
    public void delete(Long id, Long tenantId) {
        ApiInterface existing = apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));

        apiInterfaceRepository.delete(existing);
        log.info("Deleted API interface id={}, tenantId={}", id, tenantId);
    }

    @Transactional(readOnly = true)
    public ApiInterface getById(Long id, Long tenantId) {
        return apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ApiInterface> listByAgent(Long agentId, Long tenantId) {
        return apiInterfaceRepository.findByAgentIdAndTenantId(agentId, tenantId);
    }

    @Transactional(readOnly = true)
    public Page<ApiInterface> listByTenant(Long tenantId, Pageable pageable) {
        return apiInterfaceRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional
    public ApiInterface toggleActive(Long id, Long tenantId, Boolean isActive) {
        ApiInterface existing = apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));

        existing.setIsActive(isActive);
        log.info("Toggled API interface id={}, tenantId={}, isActive={}", id, tenantId, isActive);
        return apiInterfaceRepository.save(existing);
    }
}
