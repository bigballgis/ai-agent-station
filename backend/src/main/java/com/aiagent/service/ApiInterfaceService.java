package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.ApiInterface;
import com.aiagent.exception.ResourceNotFoundException;
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

    @Transactional(rollbackFor = Exception.class)
    public ApiInterface create(ApiInterface apiInterface) {
        log.info("Creating API interface for agentId={}, tenantId={}", apiInterface.getAgentId(), apiInterface.getTenantId());
        return apiInterfaceRepository.save(apiInterface);
    }

    /**
     * 版本化更新：创建新版本而非原地修改
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiInterface createNewVersion(Long id, Long tenantId, ApiInterface apiInterface) {
        ApiInterface existing = apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException());

        // 创建新版本
        ApiInterface newVersion = new ApiInterface();
        newVersion.setTenantId(existing.getTenantId());
        newVersion.setAgentId(apiInterface.getAgentId() != null ? apiInterface.getAgentId() : existing.getAgentId());
        newVersion.setVersionId(apiInterface.getVersionId() != null ? apiInterface.getVersionId() : existing.getVersionId());
        newVersion.setPath(apiInterface.getPath() != null ? apiInterface.getPath() : existing.getPath());
        newVersion.setMethod(apiInterface.getMethod() != null ? apiInterface.getMethod() : existing.getMethod());
        newVersion.setDescription(apiInterface.getDescription() != null ? apiInterface.getDescription() : existing.getDescription());
        newVersion.setIsActive(apiInterface.getIsActive() != null ? apiInterface.getIsActive() : existing.getIsActive());
        newVersion.setDeprecated(false);
        newVersion.setBaseApiId(existing.getBaseApiId() != null ? existing.getBaseApiId() : existing.getId());

        // 自动递增版本号
        String currentVersion = existing.getApiVersion() != null ? existing.getApiVersion() : "v1";
        newVersion.setApiVersion(incrementVersion(currentVersion));

        log.info("Created new API version for baseApiId={}, newVersion={}, tenantId={}",
                newVersion.getBaseApiId(), newVersion.getApiVersion(), tenantId);
        return apiInterfaceRepository.save(newVersion);
    }

    private String incrementVersion(String version) {
        try {
            String numPart = version.replaceAll("[^0-9]", "");
            int num = Integer.parseInt(numPart);
            return "v" + (num + 1);
        } catch (NumberFormatException e) {
            return "v2";
        }
    }

    /**
     * 原地更新（仅更新非核心字段如 deprecated、isActive）
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiInterface update(Long id, Long tenantId, ApiInterface apiInterface) {
        ApiInterface existing = apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException());

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
        if (apiInterface.getDeprecated() != null) {
            existing.setDeprecated(apiInterface.getDeprecated());
        }
        if (apiInterface.getDeprecationMessage() != null) {
            existing.setDeprecationMessage(apiInterface.getDeprecationMessage());
        }

        log.info("Updated API interface id={}, tenantId={}", id, tenantId);
        return apiInterfaceRepository.save(existing);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ApiInterface> listVersions(Long baseApiId, Long tenantId) {
        return apiInterfaceRepository.findByBaseApiIdAndTenantIdOrderByApiVersionDesc(baseApiId, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiInterface deprecate(Long id, Long tenantId, String message) {
        ApiInterface existing = apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException());
        existing.setDeprecated(true);
        existing.setDeprecationMessage(message);
        log.info("Deprecated API interface id={}, tenantId={}, message={}", id, tenantId, message);
        return apiInterfaceRepository.save(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long tenantId) {
        ApiInterface existing = apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException());

        apiInterfaceRepository.delete(existing);
        log.info("Deleted API interface id={}, tenantId={}", id, tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ApiInterface getById(Long id, Long tenantId) {
        return apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ApiInterface> listByAgent(Long agentId, Long tenantId) {
        return apiInterfaceRepository.findByAgentIdAndTenantId(agentId, tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Page<ApiInterface> listByTenant(Long tenantId, Pageable pageable) {
        return apiInterfaceRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiInterface toggleActive(Long id, Long tenantId, Boolean isActive) {
        ApiInterface existing = apiInterfaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException());

        existing.setIsActive(isActive);
        log.info("Toggled API interface id={}, tenantId={}, isActive={}", id, tenantId, isActive);
        return apiInterfaceRepository.save(existing);
    }
}
