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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(PermissionAspect.class);

    /**
     * Redis缓存前缀，用于缓存用户权限集合。
     * 注意：当用户角色或权限发生变更时，必须清除对应的缓存（DEL user:permissions:{userId}），
     * 否则会导致权限变更不立即生效。建议在角色/权限管理的Service层中调用 evictUserPermissionCache()。
     */
    private static final String PERMISSION_CACHE_PREFIX = "user:permissions:";
    private static final long PERMISSION_CACHE_TTL_MINUTES = 5;

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final StringRedisTemplate stringRedisTemplate;

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

        // 先查Redis缓存
        String cacheKey = PERMISSION_CACHE_PREFIX + userId;
        String cachedPermissions = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedPermissions != null) {
            // 缓存命中：检查缓存中是否包含所需权限
            Set<String> permissionNames = Set.of(cachedPermissions.split(","));
            // 如果缓存值包含通配符（SUPER_ADMIN），直接通过
            if (permissionNames.contains("*")) {
                log.debug("用户 {} 拥有 SUPER_ADMIN 角色（缓存命中），权限检查通过: {}", userId, requiredPermissionName);
                return;
            }
            if (permissionNames.contains(requiredPermissionName)) {
                log.debug("用户 {} 权限检查通过（缓存命中）: {}", userId, requiredPermissionName);
                return;
            }
            // 缓存中不包含所需权限，直接拒绝（缓存期间权限未变）
            log.warn("用户 {} 权限不足（缓存命中），缺少权限: {}", userId, requiredPermissionName);
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }

        // 缓存未命中：查询数据库
        Set<String> userPermissionNames = loadUserPermissionsFromDb(userId, requiredPermissionName);

        // 写入Redis缓存（TTL 5分钟）
        if (!userPermissionNames.isEmpty()) {
            String permissionsValue = String.join(",", userPermissionNames);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    permissionsValue,
                    PERMISSION_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
            log.debug("用户 {} 权限已缓存（{}项，TTL {}分钟）", userId, userPermissionNames.size(), PERMISSION_CACHE_TTL_MINUTES);
        }
    }

    /**
     * 从数据库加载用户的所有权限名称，并检查是否包含所需权限。
     * 如果检查通过返回用户的权限名称集合，否则抛出异常。
     */
    private Set<String> loadUserPermissionsFromDb(Long userId, String requiredPermissionName) {
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
                // SUPER_ADMIN 拥有所有权限，用通配符标记
                return Set.of("*");
            }
        }

        // 查询所有角色关联的权限
        Set<String> permissionNames = java.util.HashSet.newHashSet(roleIds.size());
        for (Long roleId : roleIds) {
            for (RolePermission rp : rolePermissionRepository.findByRoleId(roleId)) {
                Permission permission = permissionRepository.findById(rp.getPermissionId()).orElse(null);
                if (permission != null) {
                    permissionNames.add(permission.getName());
                    if (requiredPermissionName.equals(permission.getName())) {
                        log.debug("用户 {} 拥有权限 {}，权限检查通过", userId, requiredPermissionName);
                    }
                }
            }
        }

        if (!permissionNames.contains(requiredPermissionName)) {
            log.warn("用户 {} 权限不足，缺少权限: {}", userId, requiredPermissionName);
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }

        return permissionNames;
    }

    /**
     * 清除指定用户的权限缓存。
     * 应在用户角色变更、权限变更时调用。
     *
     * @param userId 用户ID
     */
    public void evictUserPermissionCache(Long userId) {
        String cacheKey = PERMISSION_CACHE_PREFIX + userId;
        Boolean deleted = stringRedisTemplate.delete(cacheKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("已清除用户 {} 的权限缓存", userId);
        }
    }
}
