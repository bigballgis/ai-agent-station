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

    List<WorkflowInstance> findByTenantIdAndStatus(Long tenantId, WorkflowInstance.InstanceStatus status);

    List<WorkflowInstance> findByWorkflowDefinitionIdAndTenantId(Long definitionId, Long tenantId);

    List<WorkflowInstance> findByWorkflowDefinitionId(Long definitionId);
}
