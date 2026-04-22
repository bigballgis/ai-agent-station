package com.aiagent.repository;

import com.aiagent.entity.WorkflowDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    List<WorkflowDefinition> findByTenantId(Long tenantId);

    Page<WorkflowDefinition> findByTenantId(Long tenantId, Pageable pageable);

    Optional<WorkflowDefinition> findByIdAndTenantId(Long id, Long tenantId);

    List<WorkflowDefinition> findByTenantIdAndStatus(Long tenantId, WorkflowDefinition.WorkflowStatus status);

    Page<WorkflowDefinition> findByTenantIdAndStatus(Long tenantId, WorkflowDefinition.WorkflowStatus status, Pageable pageable);
}
