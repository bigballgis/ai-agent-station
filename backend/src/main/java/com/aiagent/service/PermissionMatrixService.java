package com.aiagent.service;

import com.aiagent.entity.PermissionMatrix;
import com.aiagent.entity.UserRole;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.PermissionMatrixRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.tenant.TenantContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionMatrixService {

    private final PermissionMatrixRepository matrixRepository;
    private final UserRoleRepository userRoleRepository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String PERM_CACHE_PREFIX = "perm:matrix:";
    private static final String PERM_TREE_CACHE_KEY = "perm:tree";
    private static final long PERM_CACHE_TTL_MINUTES = 30;

    /**
     * Get all permissions for a role as a matrix (grouped by resource type)
     */
    public Map<String, List<PermissionMatrix>> getRolePermissions(Long roleId) {
        // Try cache first
        String cacheKey = PERM_CACHE_PREFIX + roleId;
        String cached = cacheService.get(cacheKey);
        if (cached != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, List<PermissionMatrix>> result = objectMapper.readValue(cached, Map.class);
                return result;
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize permission cache for role: {}", roleId);
            }
        }

        List<PermissionMatrix> permissions = matrixRepository.findByRoleId(roleId);

        Map<String, List<PermissionMatrix>> grouped = permissions.stream()
                .collect(Collectors.groupingBy(PermissionMatrix::getResourceType,
                        LinkedHashMap::new, Collectors.toList()));

        // Cache the result
        try {
            cacheService.set(cacheKey, objectMapper.writeValueAsString(grouped), PERM_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize permission cache for role: {}", roleId);
        }

        return grouped;
    }

    /**
     * Get all permissions for a role as a flat list
     */
    public List<PermissionMatrix> getRolePermissionsList(Long roleId) {
        return matrixRepository.findByRoleId(roleId);
    }

    /**
     * Check if a user has a specific permission
     */
    public boolean checkPermission(Long userId, String resourceType, String resourceId, String permissionCode) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return false;
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        // Check DENY rules first (deny takes precedence)
        for (Long roleId : roleIds) {
            Optional<PermissionMatrix> denyRule = matrixRepository
                    .findByRoleIdAndResourceTypeAndResourceIdAndPermissionCode(roleId, resourceType, resourceId, permissionCode);

            if (denyRule.isPresent()) {
                return "ALLOW".equals(denyRule.get().getPermissionType());
            }
        }

        // Check for wildcard resource permissions
        for (Long roleId : roleIds) {
            Optional<PermissionMatrix> wildcardRule = matrixRepository
                    .findByRoleIdAndResourceTypeAndResourceIdAndPermissionCode(roleId, resourceType, "*", permissionCode);

            if (wildcardRule.isPresent()) {
                return "ALLOW".equals(wildcardRule.get().getPermissionType());
            }
        }

        return false;
    }

    /**
     * Check if user has permission by permission code only (simplified)
     */
    public boolean checkPermissionByCode(Long userId, String permissionCode) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return false;
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        for (Long roleId : roleIds) {
            List<PermissionMatrix> perms = matrixRepository.findByRoleIdAndPermissionCode(roleId, permissionCode);
            for (PermissionMatrix perm : perms) {
                if ("ALLOW".equals(perm.getPermissionType())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Grant a permission to a role
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionMatrix grantPermission(Long roleId, String resourceType, String resourceId,
                                            String permissionCode, String permissionName) {
        // Check if already exists
        Optional<PermissionMatrix> existing = matrixRepository
                .findByRoleIdAndResourceTypeAndResourceIdAndPermissionCode(roleId, resourceType, resourceId, permissionCode);

        if (existing.isPresent()) {
            // Update existing
            PermissionMatrix perm = existing.get();
            perm.setPermissionType("ALLOW");
            perm.setPermissionName(permissionName);
            evictRolePermissionCache(roleId);
            return matrixRepository.save(perm);
        }

        PermissionMatrix perm = new PermissionMatrix();
        perm.setRoleId(roleId);
        perm.setResourceType(resourceType);
        perm.setResourceId(resourceId);
        perm.setPermissionCode(permissionCode);
        perm.setPermissionName(permissionName);
        perm.setPermissionType("ALLOW");
        perm.setTenantId(TenantContextHolder.getTenantId());

        evictRolePermissionCache(roleId);
        return matrixRepository.save(perm);
    }

    /**
     * Revoke a permission from a role
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokePermission(Long roleId, String resourceType, String resourceId, String permissionCode) {
        matrixRepository.deleteByRoleIdAndResourceTypeAndResourceIdAndPermissionCode(
                roleId, resourceType, resourceId, permissionCode);
        evictRolePermissionCache(roleId);
    }

    /**
     * Deny a permission for a role
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionMatrix denyPermission(Long roleId, String resourceType, String resourceId,
                                           String permissionCode, String permissionName) {
        Optional<PermissionMatrix> existing = matrixRepository
                .findByRoleIdAndResourceTypeAndResourceIdAndPermissionCode(roleId, resourceType, resourceId, permissionCode);

        if (existing.isPresent()) {
            PermissionMatrix perm = existing.get();
            perm.setPermissionType("DENY");
            perm.setPermissionName(permissionName);
            evictRolePermissionCache(roleId);
            return matrixRepository.save(perm);
        }

        PermissionMatrix perm = new PermissionMatrix();
        perm.setRoleId(roleId);
        perm.setResourceType(resourceType);
        perm.setResourceId(resourceId);
        perm.setPermissionCode(permissionCode);
        perm.setPermissionName(permissionName);
        perm.setPermissionType("DENY");
        perm.setTenantId(TenantContextHolder.getTenantId());

        evictRolePermissionCache(roleId);
        return matrixRepository.save(perm);
    }

    /**
     * Batch update permissions for a role
     * Replaces all existing permissions with the new set
     */
    @Transactional(rollbackFor = Exception.class)
    public List<PermissionMatrix> batchUpdatePermissions(Long roleId, List<PermissionMatrix> permissions) {
        // Delete all existing permissions for this role
        matrixRepository.deleteByRoleId(roleId);

        Long tenantId = TenantContextHolder.getTenantId();
        List<PermissionMatrix> saved = new ArrayList<>();

        for (PermissionMatrix perm : permissions) {
            perm.setId(null);
            perm.setRoleId(roleId);
            perm.setTenantId(tenantId);
            perm.setCreatedAt(LocalDateTime.now());
            saved.add(matrixRepository.save(perm));
        }

        evictRolePermissionCache(roleId);
        evictPermissionTreeCache();
        return saved;
    }

    /**
     * Get full permission tree for UI rendering
     * Returns a hierarchical structure of all resource types and their permissions
     */
    public List<Map<String, Object>> getPermissionTree() {
        // Try cache
        String cached = cacheService.get(PERM_TREE_CACHE_KEY);
        if (cached != null) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> result = objectMapper.readValue(cached, List.class);
                return result;
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize permission tree cache");
            }
        }

        Long tenantId = TenantContextHolder.getTenantId();
        List<PermissionMatrix> allPerms = matrixRepository.findByTenantId(tenantId);

        // Group by resource type
        Map<String, List<PermissionMatrix>> grouped = allPerms.stream()
                .collect(Collectors.groupingBy(PermissionMatrix::getResourceType, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> tree = new ArrayList<>();

        for (Map.Entry<String, List<PermissionMatrix>> entry : grouped.entrySet()) {
            Map<String, Object> resourceNode = new LinkedHashMap<>();
            resourceNode.put("resourceType", entry.getKey());
            resourceNode.put("label", getResourceTypeLabel(entry.getKey()));

            // Group permissions by role within this resource type
            Map<Long, List<PermissionMatrix>> byRole = entry.getValue().stream()
                    .collect(Collectors.groupingBy(PermissionMatrix::getRoleId));

            List<Map<String, Object>> roleNodes = new ArrayList<>();
            for (Map.Entry<Long, List<PermissionMatrix>> roleEntry : byRole.entrySet()) {
                Map<String, Object> roleNode = new LinkedHashMap<>();
                roleNode.put("roleId", roleEntry.getKey());
                roleNode.put("permissions", roleEntry.getValue());
                roleNodes.add(roleNode);
            }

            resourceNode.put("roles", roleNodes);
            tree.add(resourceNode);
        }

        // Cache
        try {
            cacheService.set(PERM_TREE_CACHE_KEY, objectMapper.writeValueAsString(tree), PERM_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize permission tree cache");
        }

        return tree;
    }

    /**
     * Get all permissions for current tenant
     */
    public List<PermissionMatrix> getAllPermissions() {
        Long tenantId = TenantContextHolder.getTenantId();
        return matrixRepository.findByTenantId(tenantId);
    }

    /**
     * Get permissions by role and resource type
     */
    public List<PermissionMatrix> getPermissionsByRoleAndResourceType(Long roleId, String resourceType) {
        return matrixRepository.findByRoleIdAndResourceType(roleId, resourceType);
    }

    // ==================== Private Helper Methods ====================

    private void evictRolePermissionCache(Long roleId) {
        cacheService.delete(PERM_CACHE_PREFIX + roleId);
    }

    private void evictPermissionTreeCache() {
        cacheService.delete(PERM_TREE_CACHE_KEY);
    }

    private String getResourceTypeLabel(String resourceType) {
        return switch (resourceType) {
            case "MENU" -> "菜单权限";
            case "BUTTON" -> "按钮权限";
            case "API" -> "接口权限";
            case "DATA" -> "数据权限";
            default -> resourceType;
        };
    }
}
