package com.aiagent.repository;

import com.aiagent.entity.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    Page<WorkflowInstance> findByTenantId(Long tenantId, Pageable pageable);

    Optional<WorkflowInstance> findByIdAndTenantId(Long id, Long tenantId);

    Page<WorkflowInstance> findByTenantIdAndStatus(Long tenantId, WorkflowInstance.InstanceStatus status, Pageable pageable);

    Page<WorkflowInstance> findByWorkflowDefinitionIdAndTenantId(Long definitionId, Long tenantId, Pageable pageable);

    List<WorkflowInstance> findByWorkflowDefinitionIdAndTenantId(Long definitionId, Long tenantId);
}
