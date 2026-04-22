package com.aiagent.repository;

import com.aiagent.entity.McpToolCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface McpToolCallLogRepository extends JpaRepository<McpToolCallLog, Long> {

    Page<McpToolCallLog> findByTenantId(Long tenantId, Pageable pageable);

    Page<McpToolCallLog> findByTenantIdAndMcpToolId(Long tenantId, Long mcpToolId, Pageable pageable);

    List<McpToolCallLog> findByApiCallLogId(Long apiCallLogId);
}
