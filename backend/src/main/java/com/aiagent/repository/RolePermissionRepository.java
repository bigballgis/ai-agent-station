package com.aiagent.repository;

import com.aiagent.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleId(Long roleId);
    List<RolePermission> findByPermissionId(Long permissionId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleId IN :roleIds")
    List<RolePermission> findByRoleIdIn(@Param("roleIds") List<Long> roleIds);

    @Transactional
    @Modifying
    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
