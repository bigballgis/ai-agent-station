package com.aiagent.dto;

import com.aiagent.entity.Role;
import com.aiagent.entity.Permission;
import com.aiagent.entity.User;

/**
 * Utility class for converting between entities and DTOs.
 * All conversion methods handle null fields gracefully.
 */
public class DTOConverter {

    private DTOConverter() {
        // Utility class - prevent instantiation
    }

    // ==================== User conversions ====================

    public static UserResponseDTO toUserResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setIsActive(user.getIsActive());
        dto.setTenantId(user.getTenantId());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    public static User toUserEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setTenantId(dto.getTenantId());
        return user;
    }

    // ==================== Role conversions ====================

    public static RoleDTO toRoleDTO(Role role) {
        if (role == null) {
            return null;
        }
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setTenantId(role.getTenantId());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }

    public static Role toRoleEntity(RoleDTO dto) {
        if (dto == null) {
            return null;
        }
        Role role = new Role();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setTenantId(dto.getTenantId());
        return role;
    }

    // ==================== Permission conversions ====================

    public static PermissionDTO toPermissionDTO(Permission permission) {
        if (permission == null) {
            return null;
        }
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        dto.setDescription(permission.getDescription());
        dto.setResourceCode(permission.getResourceCode());
        dto.setActionCode(permission.getActionCode());
        dto.setTenantId(permission.getTenantId());
        dto.setCreatedAt(permission.getCreatedAt());
        dto.setUpdatedAt(permission.getUpdatedAt());
        return dto;
    }

    public static Permission toPermissionEntity(PermissionDTO dto) {
        if (dto == null) {
            return null;
        }
        Permission permission = new Permission();
        permission.setName(dto.getName());
        permission.setDescription(dto.getDescription());
        permission.setResourceCode(dto.getResourceCode());
        permission.setActionCode(dto.getActionCode());
        permission.setTenantId(dto.getTenantId());
        return permission;
    }
}
