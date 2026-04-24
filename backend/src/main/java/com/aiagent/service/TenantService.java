package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Tenant;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.TenantRepository;
import com.aiagent.security.ApiKeyService;
import com.aiagent.security.annotation.Auditable;
import com.aiagent.tenant.TenantContextHolder;
import com.aiagent.util.CryptoUtils;
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
    private final CryptoUtils cryptoUtils;

    @Value("${ai-agent.tenant.schema-prefix:t_}")
    private String schemaPrefix;

    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        // 解密 API Key 和 Secret 返回给前端
        tenants.forEach(this::decryptTenantKeys);
        return tenants;
    }

    public Tenant getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.TENANT_NOT_FOUND));
        decryptTenantKeys(tenant);
        return tenant;
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "tenant", description = "创建租户")
    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.findByName(tenant.getName()).isPresent()) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "租户名称已存在");
        }

        String apiKey = UUID.randomUUID().toString();
        String apiSecret = UUID.randomUUID().toString();

        // 加密存储 API Key 和 Secret
        tenant.setApiKey(cryptoUtils.encrypt(apiKey));
        tenant.setApiSecret(cryptoUtils.encrypt(apiSecret));
        tenant.setIsActive(true);

        Tenant savedTenant = tenantRepository.save(tenant);
        savedTenant.setSchemaName(schemaPrefix + savedTenant.getId());
        tenantRepository.save(savedTenant);

        apiKeyService.saveApiKey(apiKey, savedTenant.getId(), 31536000L);

        // 返回解密后的数据
        decryptTenantKeys(savedTenant);
        return savedTenant;
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "tenant", description = "更新租户")
    public Tenant updateTenant(Long id, Tenant tenantDetails) {
        Tenant tenant = getTenantById(id);
        tenant.setName(tenantDetails.getName());
        tenant.setDescription(tenantDetails.getDescription());
        return tenantRepository.save(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "tenant", description = "删除租户")
    public void deleteTenant(Long id) {
        Tenant tenant = getTenantById(id);
        tenant.setIsActive(false);
        tenantRepository.save(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public Tenant regenerateApiKey(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.TENANT_NOT_FOUND));

        if (tenant.getApiKey() != null) {
            // 解密旧的 API Key 用于撤销
            String oldApiKey = cryptoUtils.decrypt(tenant.getApiKey());
            apiKeyService.revokeApiKey(oldApiKey);
        }

        String newApiKey = UUID.randomUUID().toString();
        String newApiSecret = UUID.randomUUID().toString();

        // 加密存储新的 API Key 和 Secret
        tenant.setApiKey(cryptoUtils.encrypt(newApiKey));
        tenant.setApiSecret(cryptoUtils.encrypt(newApiSecret));

        apiKeyService.saveApiKey(newApiKey, tenant.getId(), 31536000L);

        Tenant savedTenant = tenantRepository.save(tenant);
        decryptTenantKeys(savedTenant);
        return savedTenant;
    }

    /**
     * 解密租户的 API Key 和 Secret（用于返回给前端）
     */
    private void decryptTenantKeys(Tenant tenant) {
        if (tenant.getApiKey() != null) {
            tenant.setApiKey(cryptoUtils.decrypt(tenant.getApiKey()));
        }
        if (tenant.getApiSecret() != null) {
            tenant.setApiSecret(cryptoUtils.decrypt(tenant.getApiSecret()));
        }
    }
}
