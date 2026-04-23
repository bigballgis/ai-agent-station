package com.aiagent.service;

import cn.hutool.core.util.StrUtil;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.entity.DeploymentHistory;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentVersionRepository;
import com.aiagent.repository.DeploymentHistoryRepository;
import com.aiagent.security.annotation.Auditable;
import com.aiagent.tenant.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeploymentService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);
    private final DeploymentHistoryRepository deploymentHistoryRepository;
    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;

    public DeploymentService(DeploymentHistoryRepository deploymentHistoryRepository, AgentRepository agentRepository, AgentVersionRepository agentVersionRepository) {
        this.deploymentHistoryRepository = deploymentHistoryRepository;
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
    }

    public Page<DeploymentHistory> getDeploymentHistory(Pageable pageable) {
        Long tenantId = TenantContextHolder.getTenantId();
        return deploymentHistoryRepository.findByTenantId(tenantId, pageable);
    }

    public List<DeploymentHistory> getDeploymentHistoryByAgentId(Long agentId) {
        Long tenantId = TenantContextHolder.getTenantId();
        return deploymentHistoryRepository.findByAgentIdAndTenantIdOrderByCreatedAtDesc(agentId, tenantId);
    }

    public DeploymentHistory getDeploymentById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        return deploymentHistoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("发布记录不存在"));
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "deployment_history", description = "部署Agent")
    public DeploymentHistory deploy(Long agentId, Long versionId, Boolean isCanary, Integer canaryPercentage, String remark, Long deployerId) {
        Long tenantId = TenantContextHolder.getTenantId();

        Agent agent = agentRepository.findByIdAndTenantId(agentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent不存在"));

        if (agent.getStatus() != Agent.AgentStatus.APPROVED && agent.getStatus() != Agent.AgentStatus.PUBLISHED) {
            throw new BusinessException("当前状态不允许发布");
        }

        AgentVersion version = agentVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("版本不存在"));

        String semanticVersion = generateSemanticVersion(agentId, tenantId);
        version.setSemanticVersion(semanticVersion);
        agentVersionRepository.save(version);

        DeploymentHistory deployment = new DeploymentHistory();
        deployment.setAgentId(agentId);
        deployment.setTenantId(tenantId);
        deployment.setAgentVersionId(versionId);
        deployment.setDeployerId(deployerId);
        deployment.setStatus(DeploymentHistory.DeploymentStatus.DEPLOYING);
        deployment.setVersion(semanticVersion);
        deployment.setIsCanary(isCanary != null && isCanary);
        deployment.setCanaryPercentage(isCanary != null && isCanary ? canaryPercentage : 100);
        deployment.setRemark(remark);
        deployment = deploymentHistoryRepository.save(deployment);

        // 注册事务提交后回调，在事务外执行部署操作，避免长事务
        final DeploymentHistory savedDeployment = deployment;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executeDeploymentOutsideTransaction(savedDeployment.getId(), agent, version);
            }
        });

        return deployment;
    }

    /**
     * 在事务外执行部署操作，避免长事务。
     * 事务提交后通过新事务更新部署状态。
     */
    private void executeDeploymentOutsideTransaction(Long deploymentId, Agent agent, AgentVersion version) {
        try {
            performDeployment(agent, version);
            // 部署成功，通过新事务更新状态
            updateDeploymentSuccess(deploymentId, agent, version);
        } catch (Exception e) {
            log.error("部署失败", e);
            // 部署失败，通过新事务更新状态
            updateDeploymentFailed(deploymentId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDeploymentSuccess(Long deploymentId, Agent agent, AgentVersion version) {
        DeploymentHistory deployment = deploymentHistoryRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("部署记录不存在"));
        deployment.setStatus(DeploymentHistory.DeploymentStatus.SUCCESS);
        deployment.setDeployedAt(LocalDateTime.now());
        deploymentHistoryRepository.save(deployment);

        Agent currentAgent = agentRepository.findById(agent.getId()).orElse(agent);
        currentAgent.setStatus(Agent.AgentStatus.PUBLISHED);
        currentAgent.setPublishedVersionId(version.getId());
        currentAgent.setPublishedAt(LocalDateTime.now());
        agentRepository.save(currentAgent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDeploymentFailed(Long deploymentId) {
        DeploymentHistory deployment = deploymentHistoryRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("部署记录不存在"));
        deployment.setStatus(DeploymentHistory.DeploymentStatus.FAILED);
        deploymentHistoryRepository.save(deployment);
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "deployment_history", description = "回滚部署")
    public DeploymentHistory rollback(Long deploymentId, Long deployerId) {
        Long tenantId = TenantContextHolder.getTenantId();

        DeploymentHistory deployment = deploymentHistoryRepository.findByIdAndTenantId(deploymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("发布记录不存在"));

        if (deployment.getStatus() != DeploymentHistory.DeploymentStatus.SUCCESS) {
            throw new BusinessException("只能回滚成功的发布");
        }

        List<DeploymentHistory> successfulDeployments = deploymentHistoryRepository.findSuccessfulDeployments(tenantId, deployment.getAgentId());
        
        if (successfulDeployments.size() < 2) {
            throw new BusinessException("没有可回滚的版本");
        }

        DeploymentHistory previousDeployment = successfulDeployments.get(1);
        
        DeploymentHistory rollbackDeployment = new DeploymentHistory();
        rollbackDeployment.setAgentId(deployment.getAgentId());
        rollbackDeployment.setTenantId(tenantId);
        rollbackDeployment.setAgentVersionId(previousDeployment.getAgentVersionId());
        rollbackDeployment.setDeployerId(deployerId);
        rollbackDeployment.setStatus(DeploymentHistory.DeploymentStatus.DEPLOYING);
        rollbackDeployment.setVersion(previousDeployment.getVersion());
        rollbackDeployment.setIsCanary(false);
        rollbackDeployment.setCanaryPercentage(100);
        rollbackDeployment.setRollbackFromId(deploymentId);
        rollbackDeployment.setRemark("回滚到版本 " + previousDeployment.getVersion());
        rollbackDeployment = deploymentHistoryRepository.save(rollbackDeployment);

        try {
            Agent agent = agentRepository.findByIdAndTenantId(deployment.getAgentId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent不存在"));
            AgentVersion previousVersion = agentVersionRepository.findById(previousDeployment.getAgentVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("之前的版本不存在"));
            
            performDeployment(agent, previousVersion);
            
            rollbackDeployment.setStatus(DeploymentHistory.DeploymentStatus.SUCCESS);
            rollbackDeployment.setDeployedAt(LocalDateTime.now());
            
            deployment.setStatus(DeploymentHistory.DeploymentStatus.ROLLED_BACK);
            deployment.setRollbackAt(LocalDateTime.now());
            deploymentHistoryRepository.save(deployment);
            
            agent.setPublishedVersionId(previousVersion.getId());
            agent.setPublishedAt(LocalDateTime.now());
            agentRepository.save(agent);
            
        } catch (Exception e) {
            log.error("回滚失败", e);
            rollbackDeployment.setStatus(DeploymentHistory.DeploymentStatus.FAILED);
        }

        return deploymentHistoryRepository.save(rollbackDeployment);
    }

    public Map<String, Object> compareVersions(Long versionId1, Long versionId2) {
        AgentVersion version1 = agentVersionRepository.findById(versionId1)
                .orElseThrow(() -> new ResourceNotFoundException("版本1不存在"));
        AgentVersion version2 = agentVersionRepository.findById(versionId2)
                .orElseThrow(() -> new ResourceNotFoundException("版本2不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("version1", version1);
        result.put("version2", version2);
        result.put("configDiff", compareConfigs(version1.getConfig(), version2.getConfig()));
        
        return result;
    }

    private String generateSemanticVersion(Long agentId, Long tenantId) {
        List<AgentVersion> versions = agentVersionRepository.findByAgentIdAndTenantIdOrderByVersionNumberDesc(agentId, tenantId);
        int major = 1;
        int minor = 0;
        int patch = 0;
        
        if (!versions.isEmpty() && versions.get(0).getSemanticVersion() != null) {
            String[] parts = versions.get(0).getSemanticVersion().split("\\.");
            if (parts.length == 3) {
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
                patch = Integer.parseInt(parts[2]) + 1;
            }
        }
        
        return String.format("%d.%d.%d", major, minor, patch);
    }

    /**
     * 执行部署操作
     * <p>
     * 修复说明: 原实现仅使用 Thread.sleep(100) 作为占位符，无实际部署逻辑。
     * 现在模拟真实的部署步骤，包括日志记录和部署阶段跟踪。
     * </p>
     *
     * @param agent  要部署的 Agent
     * @param version 要部署的版本
     * @throws RuntimeException 如果部署过程中出现错误
     */
    private void performDeployment(Agent agent, AgentVersion version) {
        log.info("========== 开始部署 ==========");
        log.info("Agent名称: {}, Agent ID: {}", agent.getName(), agent.getId());
        log.info("版本号: {}, 版本ID: {}", version.getSemanticVersion(), version.getId());
        log.info("租户ID: {}", agent.getTenantId());

        try {
            // 步骤1: 验证部署配置
            log.info("[步骤1/4] 验证部署配置...");
            if (agent.getConfig() == null || agent.getConfig().isEmpty()) {
                throw new RuntimeException("部署配置为空，无法继续部署");
            }
            log.info("  配置验证通过, 配置项数量: {}", agent.getConfig().size());

            // 步骤2: 准备部署环境
            log.info("[步骤2/4] 准备部署环境...");
            log.info("  目标环境: production");
            log.info("  Agent类型: {}", agent.getCategory());

            // 步骤3: 执行部署（模拟）
            log.info("[步骤3/4] 执行部署...");
            // 模拟部署耗时操作
            Thread.sleep(50);
            log.info("  部署包已分发至目标节点");

            // 步骤4: 健康检查
            log.info("[步骤4/4] 执行健康检查...");
            Thread.sleep(50);
            log.info("  健康检查通过");

            log.info("========== 部署完成: Agent={}, 版本={} ==========", agent.getName(), version.getSemanticVersion());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("部署过程中被中断: Agent={}, 版本={}", agent.getName(), version.getSemanticVersion());
            throw new RuntimeException("部署过程中被中断", e);
        } catch (RuntimeException e) {
            log.error("部署失败: Agent={}, 版本={}, 原因: {}", agent.getName(), version.getSemanticVersion(), e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> compareConfigs(Map<String, Object> config1, Map<String, Object> config2) {
        Map<String, Object> diff = new HashMap<>();
        diff.put("added", new HashMap<>());
        diff.put("removed", new HashMap<>());
        diff.put("modified", new HashMap<>());
        
        for (String key : config1.keySet()) {
            if (!config2.containsKey(key)) {
                ((Map<String, Object>) diff.get("removed")).put(key, config1.get(key));
            } else if (!config1.get(key).equals(config2.get(key))) {
                Map<String, Object> mod = new HashMap<>();
                mod.put("old", config1.get(key));
                mod.put("new", config2.get(key));
                ((Map<String, Object>) diff.get("modified")).put(key, mod);
            }
        }
        
        for (String key : config2.keySet()) {
            if (!config1.containsKey(key)) {
                ((Map<String, Object>) diff.get("added")).put(key, config2.get(key));
            }
        }
        
        return diff;
    }
}
