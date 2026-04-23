package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Role;
import com.aiagent.entity.User;
import com.aiagent.entity.UserRole;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.RoleRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.security.annotation.Auditable;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public List<Role> getAllRoles() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return roleRepository.findByTenantId(tenantId);
        }
        return roleRepository.findAll();
    }

    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(), "角色不存在"));
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "role", description = "创建角色")
    public Role createRole(Role role) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            role.setTenantId(tenantId);
        }

        if (tenantId != null && roleRepository.findByNameAndTenantId(role.getName(), tenantId).isPresent()) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "角色名称已存在");
        }

        return roleRepository.save(role);
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "role", description = "更新角色")
    public Role updateRole(Long id, Role roleDetails) {
        Role role = getRoleById(id);
        role.setName(roleDetails.getName());
        role.setDescription(roleDetails.getDescription());
        return roleRepository.save(role);
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "role", description = "删除角色")
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoleToUser(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleRepository.save(userRole);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    public List<UserRole> getUserRoles(Long userId) {
        return userRoleRepository.findByUserId(userId);
    }
}
