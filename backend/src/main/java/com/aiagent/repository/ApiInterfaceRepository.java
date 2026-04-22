package com.aiagent.repository;

import com.aiagent.entity.ApiInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiInterfaceRepository extends JpaRepository<ApiInterface, Long> {

    List<ApiInterface> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    Page<ApiInterface> findByTenantId(Long tenantId, Pageable pageable);

    Optional<ApiInterface> findByIdAndTenantId(Long id, Long tenantId);

    List<ApiInterface> findByAgentIdAndTenantIdAndIsActive(Long agentId, Long tenantId, Boolean isActive);
}
