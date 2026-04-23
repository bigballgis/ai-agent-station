package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentVersionRepository;
import com.aiagent.security.UserPrincipal;
import com.aiagent.security.annotation.Auditable;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<Agent> getAllAgents() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return agentRepository.findByTenantId(tenantId);
        }
        return agentRepository.findAll();
    }

    /**
     * 数据库层面分页查询 Agent，支持关键词搜索和状态过滤
     */
    public Page<Agent> getAgentsPaged(Long tenantId, String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return agentRepository.findByTenantIdWithFilters(tenantId, keyword, status, pageable);
    }

    public Agent getAgentById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return agentRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
        }
        return agentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
    }

    public Optional<Agent> findAgentById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return agentRepository.findByIdAndTenantId(id, tenantId);
        }
        return agentRepository.findById(id);
    }

    public Optional<AgentVersion> findVersionById(Long id) {
        return agentVersionRepository.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "agent", description = "创建Agent")
    public Agent createAgent(Agent agent) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = getCurrentUserId();

        if (tenantId != null) {
            agent.setTenantId(tenantId);
        }

        if (agentRepository.existsByNameAndTenantId(agent.getName(), tenantId)) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "Agent名称已存在");
        }

        agent.setCreatedBy(userId);
        agent.setUpdatedBy(userId);
        agent.setIsActive(true);

        if (agent.getConfig() == null) {
            agent.setConfig(new HashMap<>());
        }

        Agent savedAgent = agentRepository.save(agent);
        createVersion(savedAgent, "初始版本");

        return savedAgent;
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "agent", description = "更新Agent")
    public Agent updateAgent(Long id, Agent agentDetails) {
        Agent agent = getAgentById(id);
        Long userId = getCurrentUserId();

        agent.setName(agentDetails.getName());
        agent.setDescription(agentDetails.getDescription());
        agent.setConfig(agentDetails.getConfig());
        agent.setIsActive(agentDetails.getIsActive());
        agent.setUpdatedBy(userId);

        Agent savedAgent = agentRepository.save(agent);
        createVersion(savedAgent, "更新配置");

        return savedAgent;
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "agent", description = "删除Agent")
    public void deleteAgent(Long id) {
        Agent agent = getAgentById(id);
        agentRepository.delete(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "agent", description = "复制Agent")
    public Agent copyAgent(Long id, String newName) {
        Agent original = getAgentById(id);
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = getCurrentUserId();

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

    public List<AgentVersion> getAgentVersions(Long agentId) {
        return agentVersionRepository.findByAgentIdOrderByVersionNumberDesc(agentId);
    }

    public AgentVersion getAgentVersion(Long agentId, Integer versionNumber) {
        return agentVersionRepository.findByAgentIdAndVersionNumber(agentId, versionNumber)
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public Agent rollbackToVersion(Long agentId, Integer versionNumber) {
        Agent agent = getAgentById(agentId);
        AgentVersion version = getAgentVersion(agentId, versionNumber);
        Long userId = getCurrentUserId();

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
        version.setCreatedBy(getCurrentUserId());

        Integer latestVersion = agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(agent.getId())
                .map(AgentVersion::getVersionNumber)
                .orElse(0);

        version.setVersionNumber(latestVersion + 1);
        agentVersionRepository.save(version);
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getId();
        }
        return null;
    }
}
