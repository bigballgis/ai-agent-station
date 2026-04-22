package com.aiagent.repository;

import com.aiagent.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    List<Role> findByTenantId(Long tenantId);
    Optional<Role> findByNameAndTenantId(String name, Long tenantId);
}
