package com.aiagent.repository;

import com.aiagent.entity.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<WorkflowInstance> findByTenantId(Long tenantId, Pageable pageable);

    Optional<WorkflowInstance> findByIdAndTenantId(Long id, Long tenantId);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<WorkflowInstance> findByTenantIdAndStatus(Long tenantId, WorkflowInstance.InstanceStatus status, Pageable pageable);

    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<WorkflowInstance> findByWorkflowDefinitionIdAndTenantId(Long definitionId, Long tenantId, Pageable pageable);

    List<WorkflowInstance> findByWorkflowDefinitionIdAndTenantId(Long definitionId, Long tenantId);

    /**
     * 统计租户下指定状态的工作流实例数量
     */
    long countByTenantIdAndStatus(Long tenantId, WorkflowInstance.InstanceStatus status);

    List<WorkflowInstance> findByTenantId(Long tenantId);

    List<WorkflowInstance> findTop1000ByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
