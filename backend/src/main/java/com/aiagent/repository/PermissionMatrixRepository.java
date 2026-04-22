package com.aiagent.repository;

import com.aiagent.entity.PermissionMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionMatrixRepository extends JpaRepository<PermissionMatrix, Long> {

    List<PermissionMatrix> findByRoleId(Long roleId);

    List<PermissionMatrix> findByRoleIdAndResourceType(Long roleId, String resourceType);

    List<PermissionMatrix> findByTenantId(Long tenantId);

    Optional<PermissionMatrix> findByRoleIdAndResourceTypeAndResourceIdAndPermissionCode(
            Long roleId, String resourceType, String resourceId, String permissionCode);

    List<PermissionMatrix> findByRoleIdAndPermissionCode(Long roleId, String permissionCode);

    @Query("SELECT DISTINCT pm.resourceType FROM PermissionMatrix pm WHERE pm.roleId = :roleId")
    List<String> findResourceTypesByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT DISTINCT pm.permissionCode FROM PermissionMatrix pm WHERE pm.roleId = :roleId AND pm.resourceType = :resourceType")
    List<String> findPermissionCodesByRoleIdAndResourceType(@Param("roleId") Long roleId, @Param("resourceType") String resourceType);

    void deleteByRoleIdAndResourceTypeAndResourceIdAndPermissionCode(
            Long roleId, String resourceType, String resourceId, String permissionCode);

    void deleteByRoleId(Long roleId);
}
