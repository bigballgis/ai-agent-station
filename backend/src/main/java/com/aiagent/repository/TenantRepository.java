package com.aiagent.repository;

import com.aiagent.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByName(String name);
    Optional<Tenant> findByApiKey(String apiKey);
    Optional<Tenant> findBySchemaName(String schemaName);
}
