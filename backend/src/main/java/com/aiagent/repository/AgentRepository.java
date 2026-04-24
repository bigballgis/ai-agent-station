package com.aiagent.repository;

import com.aiagent.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    @EntityGraph(attributePaths = {"tenant"})
    List<Agent> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<Agent> findByIdAndTenantId(Long id, Long tenantId);

    List<Agent> findByTenantIdAndIsActive(Long tenantId, Boolean isActive);

    boolean existsByNameAndTenantId(String name, Long tenantId);

    /**
     * 分页查询 Agent，支持关键词搜索、状态和类型过滤
     */
    @Query("SELECT a FROM Agent a WHERE a.tenantId = :tenantId AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status)")
    @EntityGraph(attributePaths = {"tenant"})
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
    Page<Agent> findTemplatesWithFilters(
        @Param("keyword") String keyword,
        @Param("category") String category,
        Pageable pageable
    );

    /**
     * 增加模板使用次数
     */
    @Query("UPDATE Agent a SET a.usageCount = a.usageCount + 1 WHERE a.id = :id")
    @Modifying
    int incrementUsageCount(@Param("id") Long id);

    /**
     * 更新模板评分
     */
    @Query("UPDATE Agent a SET a.rating = :rating WHERE a.id = :id")
    @Modifying
    int updateRating(@Param("id") Long id, @Param("rating") Double rating);
}
