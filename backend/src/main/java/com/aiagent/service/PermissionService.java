package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Permission;
import com.aiagent.entity.RolePermission;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.PermissionRepository;
import com.aiagent.repository.RolePermissionRepository;
import com.aiagent.tenant.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionService(PermissionRepository permissionRepository, RolePermissionRepository rolePermissionRepository) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Cacheable(value = "permissionList", key = "T(com.aiagent.tenant.TenantContextHolder).getTenantId()")
    public List<Permission> getAllPermissions() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return permissionRepository.findByTenantId(tenantId);
        }
        return permissionRepository.findAll();
    }

    public Permission getPermissionById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(), "权限不存在"));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "permissionList", allEntries = true)
    public Permission createPermission(Permission permission) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            permission.setTenantId(tenantId);
        }

        if (tenantId != null && permissionRepository.findByNameAndTenantId(permission.getName(), tenantId).isPresent()) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "权限名称已存在");
        }

        return permissionRepository.save(permission);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "permissionList", allEntries = true)
    public Permission updatePermission(Long id, Permission permissionDetails) {
        Permission permission = getPermissionById(id);
        permission.setName(permissionDetails.getName());
        permission.setDescription(permissionDetails.getDescription());
        permission.setResourceCode(permissionDetails.getResourceCode());
        permission.setActionCode(permissionDetails.getActionCode());
        return permissionRepository.save(permission);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "permissionList", allEntries = true)
    public void deletePermission(Long id) {
        permissionRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        rolePermissionRepository.save(rolePermission);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    public List<RolePermission> getRolePermissions(Long roleId) {
        return rolePermissionRepository.findByRoleId(roleId);
    }
}
