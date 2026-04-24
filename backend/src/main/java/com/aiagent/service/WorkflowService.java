package com.aiagent.service;

import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.entity.WorkflowInstance;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.WorkflowDefinitionRepository;
import com.aiagent.repository.WorkflowInstanceRepository;
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

    /**
     * 分页查询指定租户下的工作流定义
     *
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页的工作流定义列表
     */
    public Page<WorkflowDefinition> listDefinitions(Long tenantId, Pageable pageable) {
        return definitionRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * 按状态分页查询指定租户下的工作流定义
     *
     * @param tenantId 租户ID
     * @param status   工作流状态
     * @param pageable 分页参数
     * @return 分页的工作流定义列表
     */
    public Page<WorkflowDefinition> listDefinitionsByStatus(Long tenantId, WorkflowDefinition.WorkflowStatus status, Pageable pageable) {
        return definitionRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    /**
     * 创建工作流定义，包含配额检查
     *
     * @param definition 工作流定义实体
     * @return 创建后的工作流定义
     * @throws BusinessException 如果配额已满
     */
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

    /**
     * 根据ID和租户ID获取工作流定义
     *
     * @param id       工作流定义ID
     * @param tenantId 租户ID
     * @return 工作流定义实体
     * @throws ResourceNotFoundException 如果工作流定义不存在
     */
    public WorkflowDefinition getDefinitionByIdAndTenantId(Long id, Long tenantId) {
        return definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("工作流定义不存在"));
    }

    /**
     * 更新工作流定义
     *
     * @param definition 工作流定义实体（需包含 ID）
     * @return 更新后的工作流定义
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition updateDefinition(WorkflowDefinition definition) {
        return definitionRepository.save(definition);
    }

    /**
     * 删除工作流定义，自动递减工作流计数
     *
     * @param definition 工作流定义实体
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefinition(WorkflowDefinition definition) {
        definitionRepository.delete(definition);
        // 递减工作流计数
        if (definition.getTenantId() != null) {
            quotaService.decrementWorkflowCount();
        }
    }

    /**
     * 发布工作流定义，将状态设置为 PUBLISHED
     *
     * @param definition 工作流定义实体
     * @return 发布后的工作流定义
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition publishDefinition(WorkflowDefinition definition) {
        definition.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        return definitionRepository.save(definition);
    }

    /**
     * 创建工作流的新版本（基于已发布定义创建新的草稿副本）
     *
     * @param definitionId 源工作流定义ID
     * @param tenantId     租户ID
     * @return 新版本的草稿工作流定义
     * @throws ResourceNotFoundException 如果源工作流定义不存在
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
     *
     * @param definitionId  当前工作流定义ID
     * @param targetVersion 目标版本号
     * @param tenantId      租户ID
     * @return 回滚后的新草稿工作流定义
     * @throws ResourceNotFoundException 如果当前工作流定义不存在
     * @throws BusinessException       如果目标版本不存在
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

    /**
     * 分页查询指定租户下的工作流实例
     *
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页的工作流实例列表
     */
    public Page<WorkflowInstance> listInstances(Long tenantId, Pageable pageable) {
        return instanceRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * 按状态分页查询指定租户下的工作流实例
     *
     * @param tenantId 租户ID
     * @param status   工作流实例状态
     * @param pageable 分页参数
     * @return 分页的工作流实例列表
     */
    public Page<WorkflowInstance> listInstancesByStatus(Long tenantId, WorkflowInstance.InstanceStatus status, Pageable pageable) {
        return instanceRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    /**
     * 按工作流定义ID分页查询工作流实例
     *
     * @param definitionId 工作流定义ID
     * @param tenantId     租户ID
     * @param pageable     分页参数
     * @return 分页的工作流实例列表
     */
    public Page<WorkflowInstance> listInstancesByDefinitionId(Long definitionId, Long tenantId, Pageable pageable) {
        return instanceRepository.findByWorkflowDefinitionIdAndTenantId(definitionId, tenantId, pageable);
    }

    // ==================== Validation ====================

    /**
     * 验证工作流节点数量是否超过限制
     *
     * @param nodes 节点配置（包含 "nodes" 键的 Map）
     * @throws BusinessException 如果节点数量超过配置的最大限制
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
     *
     * @param edges 边配置（包含 "edges" 键的 Map）
     * @throws BusinessException 如果边数量超过配置的最大限制
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

    /**
     * 检查指定租户下是否存在指定名称的工作流定义
     *
     * @param name     工作流名称
     * @param tenantId 租户ID
     * @return 如果存在返回 true
     */
    public boolean existsByNameAndTenantId(String name, Long tenantId) {
        return definitionRepository.existsByNameAndTenantId(name, tenantId);
    }
}
