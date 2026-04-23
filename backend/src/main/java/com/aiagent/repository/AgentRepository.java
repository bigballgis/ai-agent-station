package com.aiagent.repository;

import com.aiagent.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
