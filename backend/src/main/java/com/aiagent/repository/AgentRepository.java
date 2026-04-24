package com.aiagent.repository;

import com.aiagent.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface AgentRepository extends JpaRepository<Agent, Long> {

    @EntityGraph(attributePaths = {"tenant"})
    List<Agent> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<Agent> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"tenant"})
    List<Agent> findByTenantIdAndIsActive(Long tenantId, Boolean isActive);

    @EntityGraph(attributePaths = {"tenant"})
    Page<Agent> findByTenantIdAndIsActive(Long tenantId, Boolean isActive, Pageable pageable);

    boolean existsByNameAndTenantId(String name, Long tenantId);

    /**
     * 分页查询 Agent，支持关键词搜索、状态和类型过滤
     */
    @Query("SELECT a FROM Agent a WHERE a.tenantId = :tenantId AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status)")
    @EntityGraph(attributePaths = {"tenant"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<Agent> findByTenantIdWithFilters(
        @Param("tenantId") Long tenantId,
        @Param("keyword") String keyword,
        @Param("status") String status,
        Pageable pageable
    );

    /**
     * 查询模板 Agent 列表（所有租户共享模板）
     */
    @Query("SELECT a FROM Agent a WHERE a.isTemplate = true AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR a.category = :category)")
    @EntityGraph(attributePaths = {"tenant"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<Agent> findTemplatesWithFilters(
        @Param("keyword") String keyword,
        @Param("category") String category,
        Pageable pageable
    );

    /**
     * 统计租户下指定状态的Agent数量
     */
    long countByTenantIdAndStatus(Long tenantId, Agent.AgentStatus status);

    /**
     * 按分类统计租户下的Agent数量
     */
    @Query("SELECT a.category, COUNT(a) FROM Agent a WHERE a.tenantId = :tenantId GROUP BY a.category")
    List<Object[]> countByTenantIdGroupByCategory(@Param("tenantId") Long tenantId);

    /**
     * 查询租户下指定时间范围内创建的Agent
     */
    @EntityGraph(attributePaths = {"tenant"})
    List<Agent> findByTenantIdAndCreatedAtBetween(Long tenantId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * 查询租户下指定时间范围内创建的Agent（分页）
     */
    @EntityGraph(attributePaths = {"tenant"})
    Page<Agent> findByTenantIdAndCreatedAtBetween(Long tenantId, java.time.LocalDateTime start, java.time.LocalDateTime end, Pageable pageable);

    /**
     * 增加模板使用次数
     */
    @Transactional
    @Modifying
    @Query("UPDATE Agent a SET a.usageCount = a.usageCount + 1 WHERE a.id = :id")
    int incrementUsageCount(@Param("id") Long id);

    /**
     * 更新模板评分
     */
    @Transactional
    @Modifying
    @Query("UPDATE Agent a SET a.rating = :rating WHERE a.id = :id")
    int updateRating(@Param("id") Long id, @Param("rating") Double rating);

    /**
     * 软删除租户下所有Agent（租户清理）
     */
    @Transactional
    @Modifying
    @Query("UPDATE Agent a SET a.deleted = true WHERE a.tenantId = :tenantId")
    int softDeleteAllByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 统计租户下Agent总数
     */
    long countByTenantId(Long tenantId);

    /**
     * 统计租户下活跃Agent数量
     */
    long countByTenantIdAndIsActiveTrue(Long tenantId);

    /**
     * 查询租户下指定分类的Agent列表
     */
    @EntityGraph(attributePaths = {"tenant"})
    List<Agent> findByTenantIdAndCategory(Long tenantId, String category);

    /**
     * 查询租户下指定分类的Agent列表（分页）
     */
    @EntityGraph(attributePaths = {"tenant"})
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    Page<Agent> findByTenantIdAndCategory(Long tenantId, String category, Pageable pageable);
}
