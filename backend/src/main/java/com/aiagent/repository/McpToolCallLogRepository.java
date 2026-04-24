package com.aiagent.repository;

import com.aiagent.entity.McpToolCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface McpToolCallLogRepository extends JpaRepository<McpToolCallLog, Long> {

    @EntityGraph(attributePaths = {"mcpTool", "apiCallLog"})
    Page<McpToolCallLog> findByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"mcpTool", "apiCallLog"})
    Page<McpToolCallLog> findByTenantIdAndMcpToolId(Long tenantId, Long mcpToolId, Pageable pageable);

    @EntityGraph(attributePaths = {"mcpTool", "apiCallLog"})
    List<McpToolCallLog> findByApiCallLogIdAndTenantId(Long apiCallLogId, Long tenantId);
}
