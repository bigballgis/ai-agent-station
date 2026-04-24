package com.aiagent.service;

import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.entity.WorkflowInstance;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.WorkflowDefinitionRepository;
import com.aiagent.repository.WorkflowInstanceRepository;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final QuotaService quotaService;

    @Value("${workflow.max-node-count:50}")
    private int maxNodeCount;

    @Value("${workflow.max-edge-count:100}")
    private int maxEdgeCount;

    // ==================== Workflow Definition ====================

    public Page<WorkflowDefinition> listDefinitions(Long tenantId, Pageable pageable) {
        return definitionRepository.findByTenantId(tenantId, pageable);
    }

    public Page<WorkflowDefinition> listDefinitionsByStatus(Long tenantId, WorkflowDefinition.WorkflowStatus status, Pageable pageable) {
        return definitionRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition createDefinition(WorkflowDefinition definition) {
        // 配额检查
        if (definition.getTenantId() != null) {
            quotaService.checkWorkflowQuota();
        }
        WorkflowDefinition saved = definitionRepository.save(definition);
        // 递增工作流计数
        if (definition.getTenantId() != null) {
            quotaService.incrementWorkflowCount();
        }
        return saved;
    }

    public WorkflowDefinition getDefinitionByIdAndTenantId(Long id, Long tenantId) {
        return definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("工作流定义不存在"));
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition updateDefinition(WorkflowDefinition definition) {
        return definitionRepository.save(definition);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDefinition(WorkflowDefinition definition) {
        definitionRepository.delete(definition);
        // 递减工作流计数
        if (definition.getTenantId() != null) {
            quotaService.decrementWorkflowCount();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition publishDefinition(WorkflowDefinition definition) {
        definition.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        return definitionRepository.save(definition);
    }

    /**
     * 创建工作流的新版本（基于已发布定义创建新的草稿副本）
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition createNewVersion(Long definitionId, Long tenantId) {
        WorkflowDefinition source = getDefinitionByIdAndTenantId(definitionId, tenantId);

        WorkflowDefinition newVersion = new WorkflowDefinition();
        newVersion.setName(source.getName());
        newVersion.setDescription(source.getDescription());
        newVersion.setVersion(source.getVersion() + 1);
        newVersion.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        newVersion.setNodes(source.getNodes());
        newVersion.setEdges(source.getEdges());
        newVersion.setTriggers(source.getTriggers());
        newVersion.setTenantId(tenantId);
        newVersion.setBaseDefinitionId(source.getBaseDefinitionId() != null ? source.getBaseDefinitionId() : source.getId());

        return definitionRepository.save(newVersion);
    }

    /**
     * 回滚工作流定义到指定版本（将目标版本的内容复制为新的草稿）
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition rollbackToVersion(Long definitionId, Long targetVersion, Long tenantId) {
        WorkflowDefinition current = getDefinitionByIdAndTenantId(definitionId, tenantId);

        // 查找目标版本
        Long baseId = current.getBaseDefinitionId() != null ? current.getBaseDefinitionId() : current.getId();
        List<WorkflowDefinition> allVersions = definitionRepository.findByTenantId(tenantId);
        WorkflowDefinition target = allVersions.stream()
                .filter(d -> d.getVersion().equals(targetVersion)
                        && (baseId.equals(d.getBaseDefinitionId()) || baseId.equals(d.getId())))
                .findFirst()
                .orElseThrow(() -> new BusinessException("目标版本 v" + targetVersion + " 不存在"));

        // 创建新草稿作为回滚结果
        WorkflowDefinition rollbackDraft = new WorkflowDefinition();
        rollbackDraft.setName(current.getName());
        rollbackDraft.setDescription(current.getDescription());
        rollbackDraft.setVersion(target.getVersion() + 1);
        rollbackDraft.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        rollbackDraft.setNodes(target.getNodes());
        rollbackDraft.setEdges(target.getEdges());
        rollbackDraft.setTriggers(target.getTriggers());
        rollbackDraft.setTenantId(tenantId);
        rollbackDraft.setBaseDefinitionId(baseId);

        return definitionRepository.save(rollbackDraft);
    }

    // ==================== Workflow Instance ====================

    public Page<WorkflowInstance> listInstances(Long tenantId, Pageable pageable) {
        return instanceRepository.findByTenantId(tenantId, pageable);
    }

    public Page<WorkflowInstance> listInstancesByStatus(Long tenantId, WorkflowInstance.InstanceStatus status, Pageable pageable) {
        return instanceRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    public Page<WorkflowInstance> listInstancesByDefinitionId(Long definitionId, Long tenantId, Pageable pageable) {
        return instanceRepository.findByWorkflowDefinitionIdAndTenantId(definitionId, tenantId, pageable);
    }

    // ==================== Validation ====================

    /**
     * 验证工作流节点数量是否超过限制
     */
    @SuppressWarnings("unchecked")
    public void validateNodeCount(Map<String, Object> nodes) {
        if (nodes == null) return;
        Object nodesObj = nodes.get("nodes");
        if (nodesObj instanceof List) {
            int count = ((List<?>) nodesObj).size();
            if (count > maxNodeCount) {
                throw new BusinessException("工作流节点数量（" + count + "）超过最大限制（" + maxNodeCount + "）");
            }
        }
    }

    /**
     * 验证工作流边数量是否超过限制
     */
    @SuppressWarnings("unchecked")
    public void validateEdgeCount(Map<String, Object> edges) {
        if (edges == null) return;
        Object edgesObj = edges.get("edges");
        if (edgesObj instanceof List) {
            int count = ((List<?>) edgesObj).size();
            if (count > maxEdgeCount) {
                throw new BusinessException("工作流边数量（" + count + "）超过最大限制（" + maxEdgeCount + "）");
            }
        }
    }
}
