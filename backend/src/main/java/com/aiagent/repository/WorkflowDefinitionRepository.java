package com.aiagent.repository;

import com.aiagent.entity.WorkflowDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    List<WorkflowDefinition> findByTenantId(Long tenantId);

    Page<WorkflowDefinition> findByTenantId(Long tenantId, Pageable pageable);

    Optional<WorkflowDefinition> findByIdAndTenantId(Long id, Long tenantId);

    List<WorkflowDefinition> findByTenantIdAndStatus(Long tenantId, WorkflowDefinition.WorkflowStatus status);

    Page<WorkflowDefinition> findByTenantIdAndStatus(Long tenantId, WorkflowDefinition.WorkflowStatus status, Pageable pageable);

    boolean existsByNameAndTenantId(String name, Long tenantId);

    /**
     * 统计租户下指定状态的工作流定义数量
     */
    long countByTenantIdAndStatus(Long tenantId, WorkflowDefinition.WorkflowStatus status);

    /**
     * 查询租户下指定时间范围内创建的工作流定义
     */
    List<WorkflowDefinition> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime start, LocalDateTime end);

    /**
     * 查询租户下指定时间范围内创建的工作流定义（分页）
     */
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<WorkflowDefinition> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 查询指定基础定义ID下的所有版本
     */
    List<WorkflowDefinition> findByBaseDefinitionIdAndTenantId(Long baseDefinitionId, Long tenantId);

    /**
     * 查询指定基础定义ID下的最新版本
     */
    @Query("SELECT w FROM WorkflowDefinition w WHERE w.baseDefinitionId = :baseDefinitionId AND w.tenantId = :tenantId ORDER BY w.version DESC")
    List<WorkflowDefinition> findByBaseDefinitionIdAndTenantIdOrderByVersionDesc(@Param("baseDefinitionId") Long baseDefinitionId, @Param("tenantId") Long tenantId);

    /**
     * 软删除租户下所有工作流定义（租户清理）
     */
    @Transactional
    @Modifying
    @Query("UPDATE WorkflowDefinition w SET w.deleted = true WHERE w.tenantId = :tenantId")
    int softDeleteAllByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 统计租户下工作流定义总数
     */
    long countByTenantId(Long tenantId);
}
