package com.aiagent.dto;

import com.aiagent.entity.Role;
import com.aiagent.entity.Permission;
import com.aiagent.entity.User;
import com.aiagent.entity.Agent;
import com.aiagent.entity.Tenant;
import com.aiagent.entity.AgentMemory;
import com.aiagent.entity.AgentTestCase;
import com.aiagent.entity.AgentTestExecution;
import com.aiagent.entity.AgentTestResult;
import com.aiagent.entity.AgentEvolutionSuggestion;
import com.aiagent.entity.AgentEvolutionExperience;
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

    // ==================== AgentTestCase conversions ====================

    public static TestCaseResponseDTO toTestCaseResponseDTO(AgentTestCase entity) {
        if (entity == null) {
            return null;
        }
        TestCaseResponseDTO dto = new TestCaseResponseDTO();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getTenantId());
        dto.setAgentId(entity.getAgentId());
        dto.setTestName(entity.getTestName());
        dto.setTestCode(entity.getTestCode());
        dto.setDescription(entity.getDescription());
        dto.setTestType(entity.getTestType());
        dto.setInputParams(entity.getInputParams());
        dto.setExpectedOutput(entity.getExpectedOutput());
        dto.setStatus(entity.getStatus());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static AgentTestCase toTestCaseEntity(CreateTestCaseRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        AgentTestCase entity = new AgentTestCase();
        entity.setTenantId(dto.getTenantId());
        entity.setAgentId(dto.getAgentId());
        entity.setTestName(dto.getTestName());
        entity.setTestCode(dto.getTestCode());
        entity.setDescription(dto.getDescription());
        entity.setTestType(dto.getTestType());
        entity.setInputParams(dto.getInputParams());
        entity.setExpectedOutput(dto.getExpectedOutput());
        entity.setStatus(dto.getStatus());
        entity.setCreatedBy(dto.getCreatedBy());
        return entity;
    }

    public static void updateTestCaseFromDTO(UpdateTestCaseRequestDTO dto, AgentTestCase entity) {
        if (dto == null || entity == null) {
            return;
        }
        if (dto.getTestName() != null) {
            entity.setTestName(dto.getTestName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getTestType() != null) {
            entity.setTestType(dto.getTestType());
        }
        if (dto.getInputParams() != null) {
            entity.setInputParams(dto.getInputParams());
        }
        if (dto.getExpectedOutput() != null) {
            entity.setExpectedOutput(dto.getExpectedOutput());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    // ==================== AgentTestExecution conversions ====================

    public static ExecutionResponseDTO toExecutionResponseDTO(AgentTestExecution entity) {
        if (entity == null) {
            return null;
        }
        ExecutionResponseDTO dto = new ExecutionResponseDTO();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getTenantId());
        dto.setAgentId(entity.getAgentId());
        dto.setTestCaseId(entity.getTestCaseId());
        dto.setExecutionType(entity.getExecutionType());
        dto.setExecutorId(entity.getExecutorId());
        dto.setStatus(entity.getStatus());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setExecutionTime(entity.getExecutionTime());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static AgentTestExecution toExecutionEntity(CreateExecutionRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        AgentTestExecution entity = new AgentTestExecution();
        entity.setTenantId(dto.getTenantId());
        entity.setAgentId(dto.getAgentId());
        entity.setTestCaseId(dto.getTestCaseId());
        entity.setExecutionType(dto.getExecutionType());
        entity.setExecutorId(dto.getExecutorId());
        return entity;
    }

    // ==================== AgentTestResult conversions ====================

    public static TestResultResponseDTO toTestResultResponseDTO(AgentTestResult entity) {
        if (entity == null) {
            return null;
        }
        TestResultResponseDTO dto = new TestResultResponseDTO();
        dto.setId(entity.getId());
        dto.setExecutionId(entity.getExecutionId());
        dto.setTenantId(entity.getTenantId());
        dto.setAgentId(entity.getAgentId());
        dto.setTestCaseId(entity.getTestCaseId());
        dto.setActualOutput(entity.getActualOutput());
        dto.setExpectedOutput(entity.getExpectedOutput());
        dto.setStatus(entity.getStatus());
        dto.setComparisonResult(entity.getComparisonResult());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static void updateTestResultFromDTO(UpdateTestResultRequestDTO dto, AgentTestResult entity) {
        if (dto == null || entity == null) {
            return;
        }
        if (dto.getActualOutput() != null) {
            entity.setActualOutput(dto.getActualOutput());
        }
        if (dto.getExpectedOutput() != null) {
            entity.setExpectedOutput(dto.getExpectedOutput());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getComparisonResult() != null) {
            entity.setComparisonResult(dto.getComparisonResult());
        }
        if (dto.getErrorMessage() != null) {
            entity.setErrorMessage(dto.getErrorMessage());
        }
    }

    // ==================== Tenant Request DTO conversions ====================

    public static Tenant toTenantEntity(CreateTenantRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Tenant tenant = new Tenant();
        tenant.setName(dto.getName());
        tenant.setDescription(dto.getDescription());
        tenant.setSchemaName(dto.getSchemaName());
        tenant.setIsActive(dto.getIsActive());
        tenant.setMaxAgents(dto.getMaxAgents());
        tenant.setMaxApiCallsPerDay(dto.getMaxApiCallsPerDay());
        tenant.setMaxTokensPerDay(dto.getMaxTokensPerDay());
        tenant.setMaxMcpCallsPerDay(dto.getMaxMcpCallsPerDay());
        tenant.setMaxStorageMb(dto.getMaxStorageMb());
        return tenant;
    }

    public static void updateTenantFromDTO(UpdateTenantRequestDTO dto, Tenant tenant) {
        if (dto == null || tenant == null) {
            return;
        }
        if (dto.getName() != null) {
            tenant.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            tenant.setDescription(dto.getDescription());
        }
        if (dto.getSchemaName() != null) {
            tenant.setSchemaName(dto.getSchemaName());
        }
        if (dto.getIsActive() != null) {
            tenant.setIsActive(dto.getIsActive());
        }
        if (dto.getMaxAgents() != null) {
            tenant.setMaxAgents(dto.getMaxAgents());
        }
        if (dto.getMaxApiCallsPerDay() != null) {
            tenant.setMaxApiCallsPerDay(dto.getMaxApiCallsPerDay());
        }
        if (dto.getMaxTokensPerDay() != null) {
            tenant.setMaxTokensPerDay(dto.getMaxTokensPerDay());
        }
        if (dto.getMaxMcpCallsPerDay() != null) {
            tenant.setMaxMcpCallsPerDay(dto.getMaxMcpCallsPerDay());
        }
        if (dto.getMaxStorageMb() != null) {
            tenant.setMaxStorageMb(dto.getMaxStorageMb());
        }
    }

    // ==================== AgentMemory conversions ====================

    public static MemoryResponseDTO toMemoryResponseDTO(AgentMemory entity) {
        if (entity == null) {
            return null;
        }
        MemoryResponseDTO dto = new MemoryResponseDTO();
        dto.setId(entity.getId());
        dto.setAgentId(entity.getAgentId());
        dto.setSessionId(entity.getSessionId());
        dto.setMemoryType(entity.getMemoryType() != null ? entity.getMemoryType().name() : null);
        dto.setContent(entity.getContent());
        dto.setSummary(entity.getSummary());
        dto.setTags(entity.getTags());
        dto.setImportance(entity.getImportance());
        dto.setAccessCount(entity.getAccessCount());
        dto.setLastAccessedAt(entity.getLastAccessedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setTenantId(entity.getTenantId());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static AgentMemory toMemoryEntity(CreateMemoryRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        AgentMemory entity = new AgentMemory();
        entity.setAgentId(dto.getAgentId());
        entity.setSessionId(dto.getSessionId());
        if (dto.getMemoryType() != null) {
            entity.setMemoryType(AgentMemory.MemoryType.valueOf(dto.getMemoryType()));
        }
        entity.setContent(dto.getContent());
        entity.setSummary(dto.getSummary());
        entity.setTags(dto.getTags());
        entity.setImportance(dto.getImportance());
        entity.setTenantId(dto.getTenantId());
        entity.setCreatedBy(dto.getCreatedBy());
        return entity;
    }

    // ==================== AgentEvolutionSuggestion conversions ====================

    public static SuggestionResponseDTO toSuggestionResponseDTO(AgentEvolutionSuggestion entity) {
        if (entity == null) {
            return null;
        }
        SuggestionResponseDTO dto = new SuggestionResponseDTO();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getTenantId());
        dto.setAgentId(entity.getAgentId());
        dto.setReflectionId(entity.getReflectionId());
        dto.setSuggestionType(entity.getSuggestionType());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setContent(entity.getContent());
        dto.setPriority(entity.getPriority());
        dto.setStatus(entity.getStatus());
        dto.setImplementationStatus(entity.getImplementationStatus());
        dto.setExpectedImpact(entity.getExpectedImpact());
        dto.setActualImpact(entity.getActualImpact());
        dto.setImplementedBy(entity.getImplementedBy());
        dto.setImplementedAt(entity.getImplementedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static AgentEvolutionSuggestion toSuggestionEntity(CreateSuggestionRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        AgentEvolutionSuggestion entity = new AgentEvolutionSuggestion();
        entity.setTenantId(dto.getTenantId());
        entity.setAgentId(dto.getAgentId());
        entity.setReflectionId(dto.getReflectionId());
        entity.setSuggestionType(dto.getSuggestionType());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setContent(dto.getContent());
        entity.setPriority(dto.getPriority());
        entity.setStatus(dto.getStatus());
        entity.setImplementationStatus(dto.getImplementationStatus());
        entity.setExpectedImpact(dto.getExpectedImpact());
        entity.setCreatedBy(dto.getCreatedBy());
        return entity;
    }

    public static void updateSuggestionFromDTO(UpdateSuggestionRequestDTO dto, AgentEvolutionSuggestion entity) {
        if (dto == null || entity == null) {
            return;
        }
        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getContent() != null) {
            entity.setContent(dto.getContent());
        }
        if (dto.getPriority() != null) {
            entity.setPriority(dto.getPriority());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getImplementationStatus() != null) {
            entity.setImplementationStatus(dto.getImplementationStatus());
        }
        if (dto.getExpectedImpact() != null) {
            entity.setExpectedImpact(dto.getExpectedImpact());
        }
        if (dto.getActualImpact() != null) {
            entity.setActualImpact(dto.getActualImpact());
        }
        if (dto.getImplementedBy() != null) {
            entity.setImplementedBy(dto.getImplementedBy());
        }
        if (dto.getUpdatedBy() != null) {
            entity.setUpdatedBy(dto.getUpdatedBy());
        }
    }

    // ==================== AgentEvolutionExperience conversions ====================

    public static ExperienceResponseDTO toExperienceResponseDTO(AgentEvolutionExperience entity) {
        if (entity == null) {
            return null;
        }
        ExperienceResponseDTO dto = new ExperienceResponseDTO();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getTenantId());
        dto.setAgentId(entity.getAgentId());
        dto.setExperienceType(entity.getExperienceType());
        dto.setExperienceCode(entity.getExperienceCode());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setContent(entity.getContent());
        dto.setTags(entity.getTags());
        dto.setUsageCount(entity.getUsageCount());
        dto.setEffectivenessScore(entity.getEffectivenessScore());
        dto.setStatus(entity.getStatus());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static AgentEvolutionExperience toExperienceEntity(CreateExperienceRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        AgentEvolutionExperience entity = new AgentEvolutionExperience();
        entity.setTenantId(dto.getTenantId());
        entity.setAgentId(dto.getAgentId());
        entity.setExperienceType(dto.getExperienceType());
        entity.setExperienceCode(dto.getExperienceCode());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setContent(dto.getContent());
        entity.setTags(dto.getTags());
        entity.setEffectivenessScore(dto.getEffectivenessScore());
        entity.setStatus(dto.getStatus());
        entity.setCreatedBy(dto.getCreatedBy());
        return entity;
    }

    public static void updateExperienceFromDTO(UpdateExperienceRequestDTO dto, AgentEvolutionExperience entity) {
        if (dto == null || entity == null) {
            return;
        }
        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getContent() != null) {
            entity.setContent(dto.getContent());
        }
        if (dto.getTags() != null) {
            entity.setTags(dto.getTags());
        }
        if (dto.getEffectivenessScore() != null) {
            entity.setEffectivenessScore(dto.getEffectivenessScore());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getUpdatedBy() != null) {
            entity.setUpdatedBy(dto.getUpdatedBy());
        }
    }
}
