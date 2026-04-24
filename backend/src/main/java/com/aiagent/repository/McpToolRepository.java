package com.aiagent.repository;

import com.aiagent.entity.McpTool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McpToolRepository extends JpaRepository<McpTool, Long> {

    @EntityGraph(attributePaths = {"agent"})
    List<McpTool> findByTenantIdAndIsActiveTrue(Long tenantId);

    @Deprecated
    List<McpTool> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"agent"})
    Page<McpTool> findByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"agent"})
    Optional<McpTool> findByTenantIdAndToolCode(Long tenantId, String toolCode);
}
