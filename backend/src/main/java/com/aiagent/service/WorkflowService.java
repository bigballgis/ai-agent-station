package com.aiagent.service;

import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.entity.WorkflowInstance;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.WorkflowDefinitionRepository;
import com.aiagent.repository.WorkflowInstanceRepository;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceRepository instanceRepository;

    // ==================== Workflow Definition ====================

    public Page<WorkflowDefinition> listDefinitions(Long tenantId, Pageable pageable) {
        return definitionRepository.findByTenantId(tenantId, pageable);
    }

    public Page<WorkflowDefinition> listDefinitionsByStatus(Long tenantId, WorkflowDefinition.WorkflowStatus status, Pageable pageable) {
        return definitionRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition createDefinition(WorkflowDefinition definition) {
        return definitionRepository.save(definition);
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
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition publishDefinition(WorkflowDefinition definition) {
        definition.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        return definitionRepository.save(definition);
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
}
