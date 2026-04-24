package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.dto.TenantQuotaUpdateDTO;
import com.aiagent.entity.Tenant;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.TenantRepository;
import com.aiagent.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final TenantRepository tenantRepository;

    // ==================== 配额检查方法 ====================

    public void checkAgentQuota() {
        Tenant tenant = getCurrentTenant();
        if (tenant.getMaxAgents() != null && tenant.getUsedAgents() >= tenant.getMaxAgents()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Agent 数量已达上限 (" + tenant.getMaxAgents() + ")");
        }
    }

    public void checkWorkflowQuota() {
        Tenant tenant = getCurrentTenant();
        if (tenant.getMaxWorkflows() != null && tenant.getUsedWorkflows() >= tenant.getMaxWorkflows()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "工作流数量已达上限 (" + tenant.getMaxWorkflows() + ")");
        }
    }

    public void checkApiCallQuota() {
        Tenant tenant = getCurrentTenant();
        if (tenant.getMaxApiCallsPerDay() != null && tenant.getUsedApiCallsToday() >= tenant.getMaxApiCallsPerDay()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "今日 API 调用次数已达上限");
        }
    }

    public void checkTokenQuota() {
        Tenant tenant = getCurrentTenant();
        if (tenant.getMaxTokensPerDay() != null && tenant.getUsedTokensToday() >= tenant.getMaxTokensPerDay()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "今日 Token 用量已达上限");
        }
    }

    public void checkMcpCallQuota() {
        Tenant tenant = getCurrentTenant();
        // MCP 调用配额暂未追踪实际用量，预留检查接口
        if (tenant.getMaxMcpCallsPerDay() != null && tenant.getMaxMcpCallsPerDay() <= 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "MCP 调用次数已达上限");
        }
    }

    /**
     * 检查存储配额（基于预估文件大小）
     * @param fileSizeBytes 预估文件大小（字节）
     */
    public void checkStorageQuota(long fileSizeBytes) {
        Tenant tenant = getCurrentTenant();
        if (tenant.getMaxStorageMb() != null) {
            long maxBytes = tenant.getMaxStorageMb() * 1024L * 1024L;
            // 当前未追踪实际存储用量，仅做单文件大小检查（不超过总配额的 10%）
            long singleFileLimit = maxBytes / 10;
            if (fileSizeBytes > singleFileLimit) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(),
                        "单文件大小超过限制（最大 " + (singleFileLimit / 1024 / 1024) + "MB）");
            }
        }
    }

    // ==================== 计数器递增/递减方法 ====================

    @Transactional(rollbackFor = Exception.class)
    public void incrementApiCallCount() {
        Tenant tenant = getCurrentTenant();
        tenant.setUsedApiCallsToday(tenant.getUsedApiCallsToday() + 1);
        tenantRepository.save(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementAgentCount() {
        Tenant tenant = getCurrentTenant();
        tenant.setUsedAgents(tenant.getUsedAgents() + 1);
        tenantRepository.save(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decrementAgentCount() {
        Tenant tenant = getCurrentTenant();
        tenant.setUsedAgents(Math.max(0, tenant.getUsedAgents() - 1));
        tenantRepository.save(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementWorkflowCount() {
        Tenant tenant = getCurrentTenant();
        tenant.setUsedWorkflows(tenant.getUsedWorkflows() + 1);
        tenantRepository.save(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decrementWorkflowCount() {
        Tenant tenant = getCurrentTenant();
        tenant.setUsedWorkflows(Math.max(0, tenant.getUsedWorkflows() - 1));
        tenantRepository.save(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementTokenUsage(long tokens) {
        Tenant tenant = getCurrentTenant();
        tenant.setUsedTokensToday(tenant.getUsedTokensToday() + tokens);
        tenantRepository.save(tenant);
    }

    // ==================== 私有辅助方法 ====================

    private Tenant getCurrentTenant() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return tenantRepository.findById(tenantId)
            .orElseThrow(() -> new BusinessException("租户不存在"));
    }

    private Tenant getTenantById(Long tenantId) {
        return tenantRepository.findById(tenantId)
            .orElseThrow(() -> new BusinessException("租户不存在: " + tenantId));
    }

    /**
     * 获取租户配额摘要
     */
    public Map<String, Object> getTenantQuota(Object tenantIdParam) {
        Long tenantId = parseTenantId(tenantIdParam);
        Tenant tenant = getTenantById(tenantId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tenantId", tenant.getId());
        summary.put("tenantName", tenant.getName());
        summary.put("agentCount", tenant.getUsedAgents());
        summary.put("agentLimit", tenant.getMaxAgents());
        summary.put("workflowCount", tenant.getUsedWorkflows());
        summary.put("workflowLimit", tenant.getMaxWorkflows());
        summary.put("apiCallCount", tenant.getUsedApiCallsToday());
        summary.put("apiCallLimit", tenant.getMaxApiCallsPerDay());
        summary.put("tokenUsage", tenant.getUsedTokensToday());
        summary.put("tokenLimit", tenant.getMaxTokensPerDay());
        summary.put("storage", 0);  // 当前未追踪实际存储用量
        summary.put("storageLimit", tenant.getMaxStorageMb());
        return summary;
    }

    /**
     * 获取租户配额详细明细
     */
    public Map<String, Object> getTenantQuotaDetails(Object tenantIdParam) {
        Long tenantId = parseTenantId(tenantIdParam);
        Tenant tenant = getTenantById(tenantId);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("tenantId", tenant.getId());
        details.put("tenantName", tenant.getName());
        details.put("isActive", tenant.getIsActive());

        // Agent 配额
        Map<String, Object> agentQuota = new LinkedHashMap<>();
        agentQuota.put("used", tenant.getUsedAgents());
        agentQuota.put("limit", tenant.getMaxAgents());
        agentQuota.put("remaining", Math.max(0, tenant.getMaxAgents() - tenant.getUsedAgents()));
        agentQuota.put("usagePercent", tenant.getMaxAgents() > 0
                ? (double) tenant.getUsedAgents() / tenant.getMaxAgents() * 100 : 0);
        details.put("agents", agentQuota);

        // 工作流配额
        Map<String, Object> workflowQuota = new LinkedHashMap<>();
        workflowQuota.put("used", tenant.getUsedWorkflows());
        workflowQuota.put("limit", tenant.getMaxWorkflows());
        workflowQuota.put("remaining", Math.max(0, tenant.getMaxWorkflows() - tenant.getUsedWorkflows()));
        workflowQuota.put("usagePercent", tenant.getMaxWorkflows() > 0
                ? (double) tenant.getUsedWorkflows() / tenant.getMaxWorkflows() * 100 : 0);
        details.put("workflows", workflowQuota);

        // API 调用配额
        Map<String, Object> apiCallQuota = new LinkedHashMap<>();
        apiCallQuota.put("used", tenant.getUsedApiCallsToday());
        apiCallQuota.put("limit", tenant.getMaxApiCallsPerDay());
        apiCallQuota.put("remaining", Math.max(0, tenant.getMaxApiCallsPerDay() - tenant.getUsedApiCallsToday()));
        apiCallQuota.put("usagePercent", tenant.getMaxApiCallsPerDay() > 0
                ? (double) tenant.getUsedApiCallsToday() / tenant.getMaxApiCallsPerDay() * 100 : 0);
        details.put("apiCalls", apiCallQuota);

        // Token 用量配额
        Map<String, Object> tokenQuota = new LinkedHashMap<>();
        tokenQuota.put("used", tenant.getUsedTokensToday());
        tokenQuota.put("limit", tenant.getMaxTokensPerDay());
        tokenQuota.put("remaining", Math.max(0, tenant.getMaxTokensPerDay() - tenant.getUsedTokensToday()));
        tokenQuota.put("usagePercent", tenant.getMaxTokensPerDay() > 0
                ? (double) tenant.getUsedTokensToday() / tenant.getMaxTokensPerDay() * 100 : 0);
        details.put("tokens", tokenQuota);

        // MCP 调用配额
        Map<String, Object> mcpQuota = new LinkedHashMap<>();
        mcpQuota.put("limit", tenant.getMaxMcpCallsPerDay());
        details.put("mcpCalls", mcpQuota);

        // 存储配额
        Map<String, Object> storageQuota = new LinkedHashMap<>();
        storageQuota.put("used", 0);
        storageQuota.put("limit", tenant.getMaxStorageMb());
        storageQuota.put("remaining", tenant.getMaxStorageMb());
        storageQuota.put("usagePercent", 0);
        details.put("storage", storageQuota);

        return details;
    }

    /**
     * 更新租户配额限制
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTenantQuota(Object tenantIdParam, TenantQuotaUpdateDTO dto) {
        Long tenantId = parseTenantId(tenantIdParam);
        Tenant tenant = getTenantById(tenantId);

        if (dto.getAgentLimit() != null) {
            tenant.setMaxAgents(dto.getAgentLimit().intValue());
        }
        if (dto.getApiCallLimit() != null) {
            tenant.setMaxApiCallsPerDay(dto.getApiCallLimit());
        }
        if (dto.getTokenLimit() != null) {
            tenant.setMaxTokensPerDay(dto.getTokenLimit());
        }
        if (dto.getStorageLimit() != null) {
            tenant.setMaxStorageMb(dto.getStorageLimit());
        }

        tenantRepository.save(tenant);
        return getTenantQuota(tenantId);
    }

    private Long parseTenantId(Object tenantIdParam) {
        if (tenantIdParam instanceof Long) return (Long) tenantIdParam;
        if (tenantIdParam instanceof Number) return ((Number) tenantIdParam).longValue();
        if (tenantIdParam instanceof String) return Long.parseLong((String) tenantIdParam);
        throw new BusinessException("无效的租户 ID: " + tenantIdParam);
    }
}
