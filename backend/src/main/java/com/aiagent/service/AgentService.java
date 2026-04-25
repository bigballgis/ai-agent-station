package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentVersionRepository;
import com.aiagent.security.UserPrincipal;
import com.aiagent.security.annotation.Auditable;
import com.aiagent.tenant.TenantContextHolder;
import com.aiagent.util.AgentConfigValidator;
import com.aiagent.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final QuotaService quotaService;
    private final AgentConfigValidator agentConfigValidator;

    /**
     * 获取当前租户下所有 Agent 列表
     * 如果未设置租户上下文，则返回所有 Agent
     *
     * @return Agent 列表
     */
    @CacheEvict(value = "agents", allEntries = true)
    public List<Agent> getAllAgents() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return agentRepository.findByTenantId(tenantId);
        }
        return agentRepository.findAll();
    }

    /**
     * 数据库层面分页查询 Agent，支持关键词搜索和状态过滤
     *
     * @param tenantId 租户ID
     * @param keyword  搜索关键词（可选）
     * @param status   Agent 状态过滤（可选）
     * @param page     页码（从0开始）
     * @param size     每页大小
     * @return 分页的 Agent 列表
     */
    public Page<Agent> getAgentsPaged(Long tenantId, String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return agentRepository.findByTenantIdWithFilters(tenantId, keyword, status, pageable);
    }

    /**
     * 根据 ID 获取 Agent，支持租户隔离
     *
     * @param id Agent ID
     * @return Agent 实体
     * @throws ResourceNotFoundException 如果 Agent 不存在或不在当前租户下
     */
    @Cacheable(value = "agents", key = "#id")
    public Agent getAgentById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return agentRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException());
        }
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException());
    }

    /**
     * 根据 ID 安全查找 Agent（返回 Optional），支持租户隔离
     *
     * @param id Agent ID
     * @return Optional 包含 Agent 实体，如果不存在则返回 empty
     */
    public Optional<Agent> findAgentById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return agentRepository.findByIdAndTenantId(id, tenantId);
        }
        return agentRepository.findById(id);
    }

    /**
     * 根据 ID 查找 Agent 版本
     *
     * @param id 版本 ID
     * @return Optional 包含 AgentVersion 实体，如果不存在则返回 empty
     */
    public Optional<AgentVersion> findVersionById(Long id) {
        return agentVersionRepository.findById(id);
    }

    /**
     * 检查指定租户下是否存在指定名称的Agent
     *
     * @param name     Agent 名称
     * @param tenantId 租户ID
     * @return 如果存在返回 true
     */
    public boolean existsByNameAndTenantId(String name, Long tenantId) {
        return agentRepository.existsByNameAndTenantId(name, tenantId);
    }

    /**
     * 创建 Agent
     * 包含配额检查、名称唯一性验证和配置验证，创建后自动生成初始版本
     *
     * @param agent Agent 实体（需设置 name、description、config 等字段）
     * @return 创建后的 Agent（含 ID）
     * @throws BusinessException 如果名称已存在或配额已满
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "agent", description = "创建Agent")
    @CacheEvict(value = "agents", allEntries = true)
    public Agent createAgent(Agent agent) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        // 配额检查
        if (tenantId != null) {
            quotaService.checkAgentQuota();
        }

        if (tenantId != null) {
            agent.setTenantId(tenantId);
        }

        if (agentRepository.existsByNameAndTenantId(agent.getName(), tenantId)) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "Agent名称已存在");
        }

        // 验证 Agent 配置
        agentConfigValidator.validateOrThrow(agent);

        agent.setCreatedBy(userId);
        agent.setUpdatedBy(userId);
        agent.setIsActive(true);

        if (agent.getConfig() == null) {
            agent.setConfig(new HashMap<>());
        }

        Agent savedAgent = agentRepository.save(agent);
        createVersion(savedAgent, "初始版本");

        // 递增 Agent 计数
        if (tenantId != null) {
            quotaService.incrementAgentCount();
        }

        return savedAgent;
    }

    /**
     * 更新 Agent 信息
     * 更新后自动生成新版本记录
     *
     * @param id           Agent ID
     * @param agentDetails 更新内容
     * @return 更新后的 Agent
     * @throws ResourceNotFoundException 如果 Agent 不存在
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "agent", description = "更新Agent")
    @CacheEvict(value = "agents", allEntries = true)
    public Agent updateAgent(Long id, Agent agentDetails) {
        Agent agent = getAgentById(id);
        Long userId = SecurityUtils.getCurrentUserId();

        agent.setName(agentDetails.getName());
        agent.setDescription(agentDetails.getDescription());
        agent.setConfig(agentDetails.getConfig());
        agent.setIsActive(agentDetails.getIsActive());
        agent.setUpdatedBy(userId);

        // 验证 Agent 配置
        agentConfigValidator.validateOrThrow(agent);

        Agent savedAgent = agentRepository.save(agent);
        createVersion(savedAgent, "更新配置");

        return savedAgent;
    }

    /**
     * 删除 Agent
     * 删除后自动递减租户 Agent 计数
     *
     * @param id Agent ID
     * @throws ResourceNotFoundException 如果 Agent 不存在
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "agent", description = "删除Agent")
    @CacheEvict(value = "agents", allEntries = true)
    public void deleteAgent(Long id) {
        Agent agent = getAgentById(id);
        agentRepository.delete(agent);
        // 递减 Agent 计数
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            quotaService.decrementAgentCount();
        }
    }

    /**
     * 复制 Agent，创建一个新 Agent 并复制原 Agent 的配置
     *
     * @param id      源 Agent ID
     * @param newName 新 Agent 的名称
     * @return 复制后的新 Agent
     * @throws ResourceNotFoundException 如果源 Agent 不存在
     * @throws BusinessException       如果新名称已存在
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "agent", description = "复制Agent")
    public Agent copyAgent(Long id, String newName) {
        Agent original = getAgentById(id);
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        if (agentRepository.existsByNameAndTenantId(newName, tenantId)) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "Agent名称已存在");
        }

        Agent copy = new Agent();
        copy.setTenantId(original.getTenantId());
        copy.setName(newName);
        copy.setDescription("复制自: " + original.getName());
        copy.setConfig(new HashMap<>(original.getConfig()));
        copy.setIsActive(true);
        copy.setCreatedBy(userId);
        copy.setUpdatedBy(userId);

        Agent savedCopy = agentRepository.save(copy);
        createVersion(savedCopy, "复制自Agent " + original.getId());

        return savedCopy;
    }

    /**
     * 获取指定 Agent 的所有版本记录（按版本号降序排列）
     *
     * @param agentId Agent ID
     * @return 版本列表
     */
    public List<AgentVersion> getAgentVersions(Long agentId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return agentVersionRepository.findByAgentIdAndTenantIdOrderByVersionNumberDesc(agentId, tenantId);
        }
        return agentVersionRepository.findByAgentIdOrderByVersionNumberDesc(agentId);
    }

    /**
     * 获取指定 Agent 的特定版本
     *
     * @param agentId       Agent ID
     * @param versionNumber 版本号
     * @return AgentVersion 实体
     * @throws ResourceNotFoundException 如果版本不存在
     */
    public AgentVersion getAgentVersion(Long agentId, Integer versionNumber) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return agentVersionRepository.findByAgentIdAndVersionNumberAndTenantId(agentId, versionNumber, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException());
        }
        return agentVersionRepository.findByAgentIdAndVersionNumber(agentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException());
    }

    /**
     * 回滚 Agent 到指定版本，将目标版本的配置复制到当前 Agent
     *
     * @param agentId       Agent ID
     * @param versionNumber 目标版本号
     * @return 回滚后的 Agent
     * @throws ResourceNotFoundException 如果 Agent 或版本不存在
     */
    @Transactional(rollbackFor = Exception.class)
    public Agent rollbackToVersion(Long agentId, Integer versionNumber) {
        Agent agent = getAgentById(agentId);
        AgentVersion version = getAgentVersion(agentId, versionNumber);
        Long userId = SecurityUtils.getCurrentUserId();

        agent.setConfig(new HashMap<>(version.getConfig()));
        agent.setUpdatedBy(userId);

        Agent savedAgent = agentRepository.save(agent);
        createVersion(savedAgent, "回滚到版本 " + versionNumber);

        return savedAgent;
    }

    private void createVersion(Agent agent, String changeLog) {
        AgentVersion version = new AgentVersion();
        version.setAgentId(agent.getId());
        version.setTenantId(agent.getTenantId());
        version.setConfig(new HashMap<>(agent.getConfig()));
        version.setChangeLog(changeLog);
        version.setCreatedBy(SecurityUtils.getCurrentUserId());

        Integer latestVersion = agentVersionRepository.findFirstByAgentIdAndTenantIdOrderByVersionNumberDesc(agent.getId(), agent.getTenantId())
                .map(AgentVersion::getVersionNumber)
                .orElse(0);

        version.setVersionNumber(latestVersion + 1);
        agentVersionRepository.save(version);
    }

    // ==================== 模板市场方法 ====================

    /**
     * 分页查询模板（所有租户共享）
     * 缓存5分钟，模板变更较少
     *
     * @param keyword  搜索关键词（可选）
     * @param category 模板分类（可选）
     * @param pageable 分页参数
     * @return 分页的模板 Agent 列表
     */
    @Cacheable(value = "templates",
            key = "T(java.util.Objects).hash(#keyword, #category, #pageable.pageNumber, #pageable.pageSize)")
    public Page<Agent> getTemplatesPaged(String keyword, String category, Pageable pageable) {
        return agentRepository.findTemplatesWithFilters(keyword, category, pageable);
    }

    /**
     * 基于模板创建新 Agent
     * 自动复制模板的配置、分类和标签，名称自动添加后缀避免冲突
     *
     * @param templateId 模板 Agent ID
     * @return 创建的新 Agent
     * @throws ResourceNotFoundException 如果模板不存在
     * @throws BusinessException       如果模板不是模板类型
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "templates", allEntries = true)
    public Agent createFromTemplate(Long templateId) {
        Agent template = agentRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException());

        if (!Boolean.TRUE.equals(template.getIsTemplate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该 Agent 不是模板");
        }

        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        String newName = template.getName() + " (副本)";
        if (agentRepository.existsByNameAndTenantId(newName, tenantId)) {
            int suffix = 1;
            while (agentRepository.existsByNameAndTenantId(newName + " " + suffix, tenantId)) {
                suffix++;
            }
            newName = newName + " " + suffix;
        }

        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName(newName);
        agent.setDescription(template.getDescription());
        agent.setConfig(new HashMap<>(template.getConfig()));
        agent.setCategory(template.getCategory());
        agent.setTags(template.getTags());
        agent.setIsActive(true);
        agent.setCreatedBy(userId);
        agent.setUpdatedBy(userId);

        Agent savedAgent = agentRepository.save(agent);
        createVersion(savedAgent, "从模板创建: " + template.getName());

        // 增加模板使用次数
        agentRepository.incrementUsageCount(templateId);

        return savedAgent;
    }

    /**
     * 为模板评分（1-5 星，使用加权平均）
     * 新评分占 30%，旧评分占 70%
     *
     * @param templateId 模板 Agent ID
     * @param rating     评分值（1-5）
     * @throws ResourceNotFoundException 如果模板不存在
     * @throws BusinessException       如果评分不在 1-5 范围内或不是模板类型
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "templates", allEntries = true)
    public void rateTemplate(Long templateId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "评分必须在 1-5 之间");
        }

        Agent template = agentRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException());

        if (!Boolean.TRUE.equals(template.getIsTemplate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该 Agent 不是模板");
        }

        // 加权平均: 新评分占 30%，旧评分占 70%
        double currentRating = template.getRating() != null ? template.getRating() : 0.0;
        double newRating = currentRating * 0.7 + rating * 0.3;
        agentRepository.updateRating(templateId, Math.round(newRating * 100.0) / 100.0);
    }
}
