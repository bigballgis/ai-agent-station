package com.aiagent.aspect;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.ResultCode;
import com.aiagent.entity.Permission;
import com.aiagent.entity.Role;
import com.aiagent.entity.RolePermission;
import com.aiagent.entity.UserRole;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.PermissionRepository;
import com.aiagent.repository.RolePermissionRepository;
import com.aiagent.repository.RoleRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(PermissionAspect.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Before("@annotation(requiresRole)")
    public void checkRole(RequiresRole requiresRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String requiredRole = "ROLE_" + requiresRole.value();
        boolean hasRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(requiredRole::equals);

        if (!hasRole) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }
    }

    @Before("@annotation(requiresPermission)")
    public void checkPermission(RequiresPermission requiresPermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String requiredPermissionName = requiresPermission.value();
        log.debug("权限检查: {}", requiredPermissionName);

        // 获取当前用户信息
        Object principal = authentication.getPrincipal();
        Long userId = null;
        if (principal instanceof UserPrincipal userPrincipal) {
            userId = userPrincipal.getUserId();
        }

        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 查询用户关联的所有角色
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            log.warn("用户 {} 没有分配任何角色，权限检查失败: {}", userId, requiredPermissionName);
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }

        // 获取角色ID集合
        Set<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());

        // 检查是否是超级管理员，SUPER_ADMIN 直接通过
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null && "SUPER_ADMIN".equals(role.getName())) {
                log.debug("用户 {} 拥有 SUPER_ADMIN 角色，权限检查通过: {}", userId, requiredPermissionName);
                return;
            }
        }

        // 查询所有角色关联的权限
        Set<Long> permissionIds = roleIds.stream()
                .flatMap(roleId -> rolePermissionRepository.findByRoleId(roleId).stream())
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());

        // 查询权限详情，检查是否包含所需权限
        for (Long permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId).orElse(null);
            if (permission != null && requiredPermissionName.equals(permission.getName())) {
                log.debug("用户 {} 拥有权限 {}，权限检查通过", userId, requiredPermissionName);
                return;
            }
        }

        log.warn("用户 {} 权限不足，缺少权限: {}", userId, requiredPermissionName);
        throw new BusinessException(ResultCode.PERMISSION_DENIED);
    }
}
