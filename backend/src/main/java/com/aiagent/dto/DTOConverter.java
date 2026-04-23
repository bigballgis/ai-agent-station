package com.aiagent.dto;

import com.aiagent.entity.Role;
import com.aiagent.entity.Permission;
import com.aiagent.entity.User;
import com.aiagent.entity.Agent;
import com.aiagent.entity.Tenant;
import com.aiagent.vo.AgentVO;
import com.aiagent.vo.TenantVO;

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

    public static User toUserEntity(CreateUserDTO dto) {
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

    public static void updateUserFromDTO(UpdateUserDTO dto, User user) {
        if (dto == null || user == null) {
            return;
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
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

    // ==================== Agent conversions ====================

    public static AgentVO toAgentVO(Agent agent) {
        if (agent == null) {
            return null;
        }
        AgentVO vo = new AgentVO();
        vo.setId(agent.getId());
        vo.setTenantId(agent.getTenantId());
        vo.setName(agent.getName());
        vo.setDescription(agent.getDescription());
        vo.setStatus(agent.getStatus() != null ? agent.getStatus().name() : null);
        vo.setCategory(agent.getCategory());
        vo.setConfig(agent.getConfig());
        vo.setIcon(agent.getIcon());
        vo.setLanguage(agent.getLanguage());
        vo.setTags(agent.getTags());
        vo.setIsActive(agent.getIsActive());
        vo.setVersion(agent.getVersion());
        vo.setPublishedVersionId(agent.getPublishedVersionId());
        vo.setPublishedAt(agent.getPublishedAt());
        vo.setCreatedAt(agent.getCreatedAt());
        vo.setUpdatedAt(agent.getUpdatedAt());
        return vo;
    }

    // ==================== Tenant conversions ====================

    public static TenantVO toTenantVO(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        TenantVO vo = new TenantVO();
        vo.setId(tenant.getId());
        vo.setName(tenant.getName());
        vo.setDescription(tenant.getDescription());
        vo.setSchemaName(tenant.getSchemaName());
        vo.setStatus(tenant.getIsActive() != null && tenant.getIsActive() ? "active" : "inactive");
        vo.setIsActive(tenant.getIsActive());
        vo.setMaxAgents(tenant.getMaxAgents());
        vo.setMaxApiCallsPerDay(tenant.getMaxApiCallsPerDay());
        vo.setMaxTokensPerDay(tenant.getMaxTokensPerDay());
        vo.setMaxMcpCallsPerDay(tenant.getMaxMcpCallsPerDay());
        vo.setMaxStorageMb(tenant.getMaxStorageMb());
        vo.setUsedAgents(tenant.getUsedAgents());
        vo.setUsedApiCallsToday(tenant.getUsedApiCallsToday());
        vo.setUsedTokensToday(tenant.getUsedTokensToday());
        vo.setCreatedAt(tenant.getCreatedAt());
        vo.setUpdatedAt(tenant.getUpdatedAt());
        return vo;
    }
}
