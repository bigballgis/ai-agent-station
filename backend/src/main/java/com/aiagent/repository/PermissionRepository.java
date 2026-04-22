package com.aiagent.repository;

import com.aiagent.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    List<Permission> findByTenantId(Long tenantId);
    Optional<Permission> findByNameAndTenantId(String name, Long tenantId);
}
