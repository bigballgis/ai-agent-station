package com.aiagent.repository;

import com.aiagent.entity.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {

    List<RateLimitConfig> findByTenantIdAndIsActiveTrue(Long tenantId);

    Optional<RateLimitConfig> findByTenantIdAndLimitTypeAndIsActiveTrue(Long tenantId, String limitType);

    Optional<RateLimitConfig> findByTenantIdAndAgentIdAndIsActiveTrue(Long tenantId, Long agentId);

    Optional<RateLimitConfig> findByTenantIdAndApiInterfaceIdAndIsActiveTrue(Long tenantId, Long apiInterfaceId);
}
