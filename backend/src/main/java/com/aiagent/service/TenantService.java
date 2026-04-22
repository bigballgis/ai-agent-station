package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Tenant;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.TenantRepository;
import com.aiagent.security.ApiKeyService;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ApiKeyService apiKeyService;

    @Value("${ai-agent.tenant.schema-prefix:t_}")
    private String schemaPrefix;

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant getTenantById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.TENANT_NOT_FOUND));
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.findByName(tenant.getName()).isPresent()) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "租户名称已存在");
        }

        String apiKey = UUID.randomUUID().toString();
        String apiSecret = UUID.randomUUID().toString();

        tenant.setApiKey(apiKey);
        tenant.setApiSecret(apiSecret);
        tenant.setIsActive(true);

        Tenant savedTenant = tenantRepository.save(tenant);
        savedTenant.setSchemaName(schemaPrefix + savedTenant.getId());
        tenantRepository.save(savedTenant);

        apiKeyService.saveApiKey(apiKey, savedTenant.getId(), 31536000L);

        return savedTenant;
    }

    @Transactional
    public Tenant updateTenant(Long id, Tenant tenantDetails) {
        Tenant tenant = getTenantById(id);
        tenant.setName(tenantDetails.getName());
        tenant.setDescription(tenantDetails.getDescription());
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(Long id) {
        Tenant tenant = getTenantById(id);
        tenant.setIsActive(false);
        tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant regenerateApiKey(Long id) {
        Tenant tenant = getTenantById(id);
        
        if (tenant.getApiKey() != null) {
            apiKeyService.revokeApiKey(tenant.getApiKey());
        }

        String newApiKey = UUID.randomUUID().toString();
        String newApiSecret = UUID.randomUUID().toString();
        tenant.setApiKey(newApiKey);
        tenant.setApiSecret(newApiSecret);

        apiKeyService.saveApiKey(newApiKey, tenant.getId(), 31536000L);

        return tenantRepository.save(tenant);
    }
}
