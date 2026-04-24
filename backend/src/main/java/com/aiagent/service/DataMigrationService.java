package com.aiagent.service;

import com.aiagent.common.Result;
import com.aiagent.entity.*;
import com.aiagent.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Migration Service
 * Provides tenant data export/import capabilities and migration status tracking.
 */
@Service
public class DataMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationService.class);

    private final ObjectMapper objectMapper;
    private final Flyway flyway;

    private final TenantRepository tenantRepository;
    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final McpToolRepository mcpToolRepository;
    private final ApiInterfaceRepository apiInterfaceRepository;
    private final AgentApprovalRepository agentApprovalRepository;
    private final DeploymentHistoryRepository deploymentHistoryRepository;
    private final RateLimitConfigRepository rateLimitConfigRepository;
    private final ApprovalChainRepository approvalChainRepository;
    private final AgentEvolutionExperienceRepository experienceRepository;
    private final AgentEvolutionSuggestionRepository suggestionRepository;
    private final AgentEvolutionReflectionRepository reflectionRepository;
    private final AgentTestCaseRepository testCaseRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final DictTypeRepository dictTypeRepository;
    private final PermissionMatrixRepository permissionMatrixRepository;

    public DataMigrationService(
            Flyway flyway,
            TenantRepository tenantRepository,
            AgentRepository agentRepository,
            AgentVersionRepository agentVersionRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowInstanceRepository workflowInstanceRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            McpToolRepository mcpToolRepository,
            ApiInterfaceRepository apiInterfaceRepository,
            AgentApprovalRepository agentApprovalRepository,
            DeploymentHistoryRepository deploymentHistoryRepository,
            RateLimitConfigRepository rateLimitConfigRepository,
            ApprovalChainRepository approvalChainRepository,
            AgentEvolutionExperienceRepository experienceRepository,
            AgentEvolutionSuggestionRepository suggestionRepository,
            AgentEvolutionReflectionRepository reflectionRepository,
            AgentTestCaseRepository testCaseRepository,
            AlertRuleRepository alertRuleRepository,
            DictTypeRepository dictTypeRepository,
            PermissionMatrixRepository permissionMatrixRepository) {
        this.flyway = flyway;
        this.tenantRepository = tenantRepository;
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.mcpToolRepository = mcpToolRepository;
        this.apiInterfaceRepository = apiInterfaceRepository;
        this.agentApprovalRepository = agentApprovalRepository;
        this.deploymentHistoryRepository = deploymentHistoryRepository;
        this.rateLimitConfigRepository = rateLimitConfigRepository;
        this.approvalChainRepository = approvalChainRepository;
        this.experienceRepository = experienceRepository;
        this.suggestionRepository = suggestionRepository;
        this.reflectionRepository = reflectionRepository;
        this.testCaseRepository = testCaseRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.dictTypeRepository = dictTypeRepository;
        this.permissionMatrixRepository = permissionMatrixRepository;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Export all tenant data as JSON
     */
    @Transactional(readOnly = true)
    public Result<String> exportTenantData(Long tenantId) {
        log.info("Starting data export for tenant: {}", tenantId);

        // Validate tenant exists
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return Result.error(404, "Tenant not found: " + tenantId);
        }

        try {
            TenantExportData exportData = new TenantExportData();
            exportData.setExportTime(LocalDateTime.now());
            exportData.setExportVersion("1.0");
            exportData.setTenantId(tenantId);
            exportData.setTenantName(tenant.getName());

            // Export core tenant data
            exportData.setTenant(sanitizeTenant(tenant));

            // Export agents and related data
            List<Agent> agents = agentRepository.findByTenantId(tenantId);
            exportData.setAgents(agents);

            // Export agent versions
            List<AgentVersion> versions = agentVersionRepository.findByTenantId(tenantId);
            exportData.setAgentVersions(versions);

            // Export API interfaces
            List<ApiInterface> apiInterfaces = apiInterfaceRepository.findByTenantId(tenantId);
            exportData.setApiInterfaces(apiInterfaces);

            // Export workflows
            List<WorkflowDefinition> workflows = workflowDefinitionRepository.findByTenantId(tenantId);
            exportData.setWorkflowDefinitions(workflows);

            // Export workflow instances (limited to recent 1000)
            List<WorkflowInstance> instances = workflowInstanceRepository.findTop1000ByTenantIdOrderByCreatedAtDesc(tenantId);
            exportData.setWorkflowInstances(instances);

            // Export users
            List<User> users = userRepository.findByTenantId(tenantId);
            exportData.setUsers(sanitizeUsers(users));

            // Export roles and permissions
            List<Role> roles = roleRepository.findByTenantId(tenantId);
            exportData.setRoles(roles);

            List<Permission> permissions = permissionRepository.findByTenantId(tenantId);
            exportData.setPermissions(permissions);

            // Export user-role mappings
            List<UserRole> userRoles = userRoleRepository.findByUserIdIn(
                    users.stream().map(User::getId).collect(Collectors.toList()));
            exportData.setUserRoles(userRoles);

            // Export role-permission mappings
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdIn(
                    roles.stream().map(Role::getId).collect(Collectors.toList()));
            exportData.setRolePermissions(rolePermissions);

            // Export MCP tools
            List<McpTool> mcpTools = mcpToolRepository.findByTenantId(tenantId);
            exportData.setMcpTools(mcpTools);

            // Export rate limit configs
            List<RateLimitConfig> rateLimitConfigs = rateLimitConfigRepository.findByTenantId(tenantId);
            exportData.setRateLimitConfigs(rateLimitConfigs);

            // Export approval chains
            List<ApprovalChain> approvalChains = approvalChainRepository.findByTenantId(tenantId);
            exportData.setApprovalChains(approvalChains);

            // Export agent approvals
            List<AgentApproval> approvals = agentApprovalRepository.findByTenantId(tenantId);
            exportData.setAgentApprovals(approvals);

            // Export deployment histories
            List<DeploymentHistory> deployments = deploymentHistoryRepository.findByTenantId(tenantId);
            exportData.setDeploymentHistories(deployments);

            // Export evolution data
            List<AgentEvolutionExperience> experiences = experienceRepository.findByTenantId(tenantId);
            exportData.setEvolutionExperiences(experiences);

            List<AgentEvolutionSuggestion> suggestions = suggestionRepository.findByTenantId(tenantId);
            exportData.setEvolutionSuggestions(suggestions);

            List<AgentEvolutionReflection> reflections = reflectionRepository.findByTenantId(tenantId);
            exportData.setEvolutionReflections(reflections);

            // Export test data
            List<AgentTestCase> testCases = testCaseRepository.findByTenantId(tenantId);
            exportData.setTestCases(testCases);

            // Export alert rules
            List<AlertRule> alertRules = alertRuleRepository.findByTenantId(tenantId);
            exportData.setAlertRules(alertRules);

            // Export permission matrix
            List<PermissionMatrix> permissionMatrix = permissionMatrixRepository.findByTenantId(tenantId);
            exportData.setPermissionMatrix(permissionMatrix);

            // Export dict types and items
            List<DictType> dictTypes = dictTypeRepository.findByTenantId(tenantId);
            exportData.setDictTypes(dictTypes);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);

            log.info("Data export completed for tenant: {}, size: {} bytes", tenantId, json.length());
            return Result.success(json);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize tenant data for tenant: {}", tenantId, e);
            return Result.error(500, "Failed to serialize tenant data: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to export tenant data for tenant: {}", tenantId, e);
            return Result.error(500, "Failed to export tenant data: " + e.getMessage());
        }
    }

    /**
     * Validate import data before actual import
     */
    public Result<Map<String, Object>> validateImportData(String json) {
        log.info("Validating import data");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            TenantExportData importData = objectMapper.readValue(json, TenantExportData.class);

            // Basic structure validation
            if (importData.getTenant() == null) {
                errors.add("Missing tenant data");
            }
            if (importData.getExportVersion() == null) {
                warnings.add("Missing export version, compatibility not guaranteed");
            }
            if (importData.getExportTime() == null) {
                warnings.add("Missing export timestamp");
            }

            // Tenant validation
            if (importData.getTenant() != null) {
                Tenant tenant = importData.getTenant();
                if (tenant.getName() == null || tenant.getName().isBlank()) {
                    errors.add("Tenant name is required");
                }
                // Check if tenant name already exists
                if (tenantRepository.findByName(tenant.getName()).isPresent()) {
                    warnings.add("Tenant name '" + tenant.getName() + "' already exists. Import will create a new tenant with a suffix.");
                }
            }

            // Users validation
            if (importData.getUsers() != null && !importData.getUsers().isEmpty()) {
                stats.put("users", importData.getUsers().size());
                for (User user : importData.getUsers()) {
                    if (user.getUsername() == null || user.getUsername().isBlank()) {
                        errors.add("User with null username found");
                    }
                }
            }

            // Agents validation
            if (importData.getAgents() != null && !importData.getAgents().isEmpty()) {
                stats.put("agents", importData.getAgents().size());
                for (Agent agent : importData.getAgents()) {
                    if (agent.getName() == null || agent.getName().isBlank()) {
                        errors.add("Agent with null name found");
                    }
                }
            }

            // Workflows validation
            if (importData.getWorkflowDefinitions() != null && !importData.getWorkflowDefinitions().isEmpty()) {
                stats.put("workflows", importData.getWorkflowDefinitions().size());
            }

            // Roles validation
            if (importData.getRoles() != null && !importData.getRoles().isEmpty()) {
                stats.put("roles", importData.getRoles().size());
            }

            // Permissions validation
            if (importData.getPermissions() != null && !importData.getPermissions().isEmpty()) {
                stats.put("permissions", importData.getPermissions().size());
            }

            // MCP Tools validation
            if (importData.getMcpTools() != null && !importData.getMcpTools().isEmpty()) {
                stats.put("mcpTools", importData.getMcpTools().size());
            }

            // API Interfaces validation
            if (importData.getApiInterfaces() != null && !importData.getApiInterfaces().isEmpty()) {
                stats.put("apiInterfaces", importData.getApiInterfaces().size());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("valid", errors.isEmpty());
            result.put("errors", errors);
            result.put("warnings", warnings);
            result.put("stats", stats);
            result.put("exportVersion", importData.getExportVersion());
            result.put("exportTime", importData.getExportTime());

            if (!errors.isEmpty()) {
                return Result.error(400, "Validation failed with " + errors.size() + " error(s)", result);
            }

            return Result.success(result);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse import data JSON", e);
            return Result.error(400, "Invalid JSON format: " + e.getMessage());
        }
    }

    /**
     * Import tenant data from JSON
     */
    @Transactional
    public Result<Map<String, Object>> importTenantData(String json) {
        log.info("Starting tenant data import");

        // First validate
        Result<Map<String, Object>> validationResult = validateImportData(json);
        if (validationResult.getCode() != 200) {
            return validationResult;
        }

        try {
            TenantExportData importData = objectMapper.readValue(json, TenantExportData.class);
            Map<String, Object> importStats = new LinkedHashMap<>();
            int totalRecords = 0;

            // Create tenant (with suffix if name exists)
            Tenant sourceTenant = importData.getTenant();
            String tenantName = sourceTenant.getName();
            if (tenantRepository.findByName(tenantName).isPresent()) {
                tenantName = tenantName + "_imported_" + System.currentTimeMillis();
            }

            Tenant newTenant = new Tenant();
            newTenant.setName(tenantName);
            newTenant.setDescription(sourceTenant.getDescription());
            newTenant.setIsActive(sourceTenant.getIsActive());
            newTenant.setMaxAgents(sourceTenant.getMaxAgents());
            newTenant.setMaxApiCallsPerDay(sourceTenant.getMaxApiCallsPerDay());
            newTenant.setMaxTokensPerDay(sourceTenant.getMaxTokensPerDay());
            newTenant.setMaxMcpCallsPerDay(sourceTenant.getMaxMcpCallsPerDay());
            newTenant.setMaxStorageMb(sourceTenant.getMaxStorageMb());
            newTenant.setMaxWorkflows(sourceTenant.getMaxWorkflows());
            newTenant = tenantRepository.save(newTenant);
            Long newTenantId = newTenant.getId();
            importStats.put("newTenantId", newTenantId);
            importStats.put("newTenantName", tenantName);
            totalRecords++;

            // Build ID mapping: old -> new
            Map<Long, Long> agentIdMap = new HashMap<>();
            Map<Long, Long> userIdMap = new HashMap<>();
            Map<Long, Long> roleIdMap = new HashMap<>();
            Map<Long, Long> permissionIdMap = new HashMap<>();
            Map<Long, Long> workflowIdMap = new HashMap<>();
            Map<Long, Long> mcpToolIdMap = new HashMap<>();
            Map<Long, Long> versionIdMap = new HashMap<>();
            Map<Long, Long> approvalChainIdMap = new HashMap<>();

            // Import roles
            if (importData.getRoles() != null) {
                for (Role role : importData.getRoles()) {
                    Long oldId = role.getId();
                    role.setId(null);
                    role.setTenantId(newTenantId);
                    Role saved = roleRepository.save(role);
                    roleIdMap.put(oldId, saved.getId());
                    totalRecords++;
                }
                importStats.put("roles", importData.getRoles().size());
            }

            // Import permissions
            if (importData.getPermissions() != null) {
                for (Permission perm : importData.getPermissions()) {
                    Long oldId = perm.getId();
                    perm.setId(null);
                    perm.setTenantId(newTenantId);
                    Permission saved = permissionRepository.save(perm);
                    permissionIdMap.put(oldId, saved.getId());
                    totalRecords++;
                }
                importStats.put("permissions", importData.getPermissions().size());
            }

            // Import role-permission mappings
            if (importData.getRolePermissions() != null) {
                for (RolePermission rp : importData.getRolePermissions()) {
                    rp.setId(null);
                    rp.setRoleId(roleIdMap.getOrDefault(rp.getRoleId(), rp.getRoleId()));
                    rp.setPermissionId(permissionIdMap.getOrDefault(rp.getPermissionId(), rp.getPermissionId()));
                    rolePermissionRepository.save(rp);
                    totalRecords++;
                }
            }

            // Import users (passwords are hashed, can be imported directly)
            if (importData.getUsers() != null) {
                for (User user : importData.getUsers()) {
                    Long oldId = user.getId();
                    user.setId(null);
                    user.setTenantId(newTenantId);
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(null);
                    User saved = userRepository.save(user);
                    userIdMap.put(oldId, saved.getId());
                    totalRecords++;
                }
                importStats.put("users", importData.getUsers().size());
            }

            // Import user-role mappings
            if (importData.getUserRoles() != null) {
                for (UserRole ur : importData.getUserRoles()) {
                    ur.setId(null);
                    ur.setUserId(userIdMap.getOrDefault(ur.getUserId(), ur.getUserId()));
                    ur.setRoleId(roleIdMap.getOrDefault(ur.getRoleId(), ur.getRoleId()));
                    userRoleRepository.save(ur);
                    totalRecords++;
                }
            }

            // Import agents
            if (importData.getAgents() != null) {
                for (Agent agent : importData.getAgents()) {
                    Long oldId = agent.getId();
                    agent.setId(null);
                    agent.setTenantId(newTenantId);
                    agent.setPublishedVersionId(null);
                    agent.setStatus(Agent.AgentStatus.DRAFT);
                    Agent saved = agentRepository.save(agent);
                    agentIdMap.put(oldId, saved.getId());
                    totalRecords++;
                }
                importStats.put("agents", importData.getAgents().size());
            }

            // Import agent versions
            if (importData.getAgentVersions() != null) {
                for (AgentVersion version : importData.getAgentVersions()) {
                    Long oldId = version.getId();
                    version.setId(null);
                    version.setTenantId(newTenantId);
                    version.setAgentId(agentIdMap.getOrDefault(version.getAgentId(), version.getAgentId()));
                    AgentVersion saved = agentVersionRepository.save(version);
                    versionIdMap.put(oldId, saved.getId());
                    totalRecords++;
                }
                importStats.put("agentVersions", importData.getAgentVersions().size());
            }

            // Import API interfaces
            if (importData.getApiInterfaces() != null) {
                for (ApiInterface api : importData.getApiInterfaces()) {
                    api.setId(null);
                    api.setTenantId(newTenantId);
                    api.setAgentId(agentIdMap.getOrDefault(api.getAgentId(), api.getAgentId()));
                    api.setVersionId(versionIdMap.getOrDefault(api.getVersionId(), api.getVersionId()));
                    apiInterfaceRepository.save(api);
                    totalRecords++;
                }
                importStats.put("apiInterfaces", importData.getApiInterfaces().size());
            }

            // Import MCP tools
            if (importData.getMcpTools() != null) {
                for (McpTool tool : importData.getMcpTools()) {
                    Long oldId = tool.getId();
                    tool.setId(null);
                    tool.setTenantId(newTenantId);
                    tool.setHealthStatus("UNKNOWN");
                    tool.setConsecutiveFailures(0);
                    McpTool saved = mcpToolRepository.save(tool);
                    mcpToolIdMap.put(oldId, saved.getId());
                    totalRecords++;
                }
                importStats.put("mcpTools", importData.getMcpTools().size());
            }

            // Import workflows
            if (importData.getWorkflowDefinitions() != null) {
                for (WorkflowDefinition wf : importData.getWorkflowDefinitions()) {
                    Long oldId = wf.getId();
                    wf.setId(null);
                    wf.setTenantId(newTenantId);
                    wf.setBaseDefinitionId(null);
                    wf.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
                    WorkflowDefinition saved = workflowDefinitionRepository.save(wf);
                    workflowIdMap.put(oldId, saved.getId());
                    totalRecords++;
                }
                importStats.put("workflows", importData.getWorkflowDefinitions().size());
            }

            // Import approval chains
            if (importData.getApprovalChains() != null) {
                for (ApprovalChain chain : importData.getApprovalChains()) {
                    Long oldId = chain.getId();
                    chain.setId(null);
                    chain.setTenantId(newTenantId);
                    ApprovalChain saved = approvalChainRepository.save(chain);
                    approvalChainIdMap.put(oldId, saved.getId());
                    totalRecords++;
                }
                importStats.put("approvalChains", importData.getApprovalChains().size());
            }

            // Import rate limit configs
            if (importData.getRateLimitConfigs() != null) {
                for (RateLimitConfig rlc : importData.getRateLimitConfigs()) {
                    rlc.setId(null);
                    rlc.setTenantId(newTenantId);
                    rlc.setAgentId(agentIdMap.getOrDefault(rlc.getAgentId(), rlc.getAgentId()));
                    rateLimitConfigRepository.save(rlc);
                    totalRecords++;
                }
            }

            // Import alert rules
            if (importData.getAlertRules() != null) {
                for (AlertRule rule : importData.getAlertRules()) {
                    rule.setId(null);
                    rule.setTenantId(newTenantId);
                    alertRuleRepository.save(rule);
                    totalRecords++;
                }
                importStats.put("alertRules", importData.getAlertRules().size());
            }

            // Import permission matrix
            if (importData.getPermissionMatrix() != null) {
                for (PermissionMatrix pm : importData.getPermissionMatrix()) {
                    pm.setId(null);
                    pm.setTenantId(newTenantId);
                    pm.setRoleId(roleIdMap.getOrDefault(pm.getRoleId(), pm.getRoleId()));
                    permissionMatrixRepository.save(pm);
                    totalRecords++;
                }
            }

            // Import dict types
            if (importData.getDictTypes() != null) {
                for (DictType dt : importData.getDictTypes()) {
                    dt.setId(null);
                    dictTypeRepository.save(dt);
                    totalRecords++;
                }
                importStats.put("dictTypes", importData.getDictTypes().size());
            }

            importStats.put("totalRecords", totalRecords);
            importStats.put("importTime", LocalDateTime.now().toString());

            log.info("Tenant data import completed. New tenant ID: {}, Total records: {}", newTenantId, totalRecords);
            return Result.success("Import completed successfully", importStats);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse import data JSON", e);
            return Result.error(400, "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to import tenant data", e);
            return Result.error(500, "Import failed: " + e.getMessage());
        }
    }

    /**
     * Get migration status from Flyway
     */
    public Result<Map<String, Object>> getMigrationStatus() {
        try {
            MigrationInfoService infoService = flyway.info();
            MigrationInfo current = infoService.current();
            MigrationInfo[] pending = infoService.pending();

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("currentMigration", current != null ? current.getVersion() + " - " + current.getDescription() : "None");
            status.put("pendingCount", pending.length);

            List<Map<String, String>> pendingList = new ArrayList<>();
            for (MigrationInfo mi : pending) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("version", mi.getVersion() != null ? mi.getVersion().toString() : "unknown");
                item.put("description", mi.getDescription());
                item.put("type", mi.getType().name());
                item.put("script", mi.getScript());
                pendingList.add(item);
            }
            status.put("pendingMigrations", pendingList);

            // Overall status
            status.put("status", pending.length == 0 ? "UP_TO_DATE" : "PENDING_MIGRATIONS");

            return Result.success(status);
        } catch (Exception e) {
            log.error("Failed to get migration status", e);
            return Result.error(500, "Failed to get migration status: " + e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private Tenant sanitizeTenant(Tenant tenant) {
        Tenant sanitized = new Tenant();
        sanitized.setId(tenant.getId());
        sanitized.setName(tenant.getName());
        sanitized.setDescription(tenant.getDescription());
        sanitized.setIsActive(tenant.getIsActive());
        sanitized.setMaxAgents(tenant.getMaxAgents());
        sanitized.setMaxApiCallsPerDay(tenant.getMaxApiCallsPerDay());
        sanitized.setMaxTokensPerDay(tenant.getMaxTokensPerDay());
        sanitized.setMaxMcpCallsPerDay(tenant.getMaxMcpCallsPerDay());
        sanitized.setMaxStorageMb(tenant.getMaxStorageMb());
        sanitized.setMaxWorkflows(tenant.getMaxWorkflows());
        sanitized.setCreatedAt(tenant.getCreatedAt());
        sanitized.setUpdatedAt(tenant.getUpdatedAt());
        return sanitized;
    }

    private List<User> sanitizeUsers(List<User> users) {
        return users.stream().map(user -> {
            User sanitized = new User();
            sanitized.setId(user.getId());
            sanitized.setTenantId(user.getTenantId());
            sanitized.setUsername(user.getUsername());
            sanitized.setPassword(user.getPassword()); // Keep hashed password
            sanitized.setEmail(user.getEmail());
            sanitized.setPhone(user.getPhone());
            sanitized.setIsActive(user.getIsActive());
            sanitized.setCreatedAt(user.getCreatedAt());
            sanitized.setUpdatedAt(user.getUpdatedAt());
            return sanitized;
        }).collect(Collectors.toList());
    }

    // ==================== Inner DTO class for export data ====================

    public static class TenantExportData {
        private String exportVersion;
        private LocalDateTime exportTime;
        private Long tenantId;
        private String tenantName;

        private Tenant tenant;
        private List<Agent> agents;
        private List<AgentVersion> agentVersions;
        private List<ApiInterface> apiInterfaces;
        private List<WorkflowDefinition> workflowDefinitions;
        private List<WorkflowInstance> workflowInstances;
        private List<User> users;
        private List<Role> roles;
        private List<Permission> permissions;
        private List<UserRole> userRoles;
        private List<RolePermission> rolePermissions;
        private List<McpTool> mcpTools;
        private List<RateLimitConfig> rateLimitConfigs;
        private List<ApprovalChain> approvalChains;
        private List<AgentApproval> agentApprovals;
        private List<DeploymentHistory> deploymentHistories;
        private List<AgentEvolutionExperience> evolutionExperiences;
        private List<AgentEvolutionSuggestion> evolutionSuggestions;
        private List<AgentEvolutionReflection> evolutionReflections;
        private List<AgentTestCase> testCases;
        private List<AlertRule> alertRules;
        private List<PermissionMatrix> permissionMatrix;
        private List<DictType> dictTypes;

        // Getters and Setters
        public String getExportVersion() { return exportVersion; }
        public void setExportVersion(String exportVersion) { this.exportVersion = exportVersion; }
        public LocalDateTime getExportTime() { return exportTime; }
        public void setExportTime(LocalDateTime exportTime) { this.exportTime = exportTime; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public String getTenantName() { return tenantName; }
        public void setTenantName(String tenantName) { this.tenantName = tenantName; }
        public Tenant getTenant() { return tenant; }
        public void setTenant(Tenant tenant) { this.tenant = tenant; }
        public List<Agent> getAgents() { return agents; }
        public void setAgents(List<Agent> agents) { this.agents = agents; }
        public List<AgentVersion> getAgentVersions() { return agentVersions; }
        public void setAgentVersions(List<AgentVersion> agentVersions) { this.agentVersions = agentVersions; }
        public List<ApiInterface> getApiInterfaces() { return apiInterfaces; }
        public void setApiInterfaces(List<ApiInterface> apiInterfaces) { this.apiInterfaces = apiInterfaces; }
        public List<WorkflowDefinition> getWorkflowDefinitions() { return workflowDefinitions; }
        public void setWorkflowDefinitions(List<WorkflowDefinition> workflowDefinitions) { this.workflowDefinitions = workflowDefinitions; }
        public List<WorkflowInstance> getWorkflowInstances() { return workflowInstances; }
        public void setWorkflowInstances(List<WorkflowInstance> workflowInstances) { this.workflowInstances = workflowInstances; }
        public List<User> getUsers() { return users; }
        public void setUsers(List<User> users) { this.users = users; }
        public List<Role> getRoles() { return roles; }
        public void setRoles(List<Role> roles) { this.roles = roles; }
        public List<Permission> getPermissions() { return permissions; }
        public void setPermissions(List<Permission> permissions) { this.permissions = permissions; }
        public List<UserRole> getUserRoles() { return userRoles; }
        public void setUserRoles(List<UserRole> userRoles) { this.userRoles = userRoles; }
        public List<RolePermission> getRolePermissions() { return rolePermissions; }
        public void setRolePermissions(List<RolePermission> rolePermissions) { this.rolePermissions = rolePermissions; }
        public List<McpTool> getMcpTools() { return mcpTools; }
        public void setMcpTools(List<McpTool> mcpTools) { this.mcpTools = mcpTools; }
        public List<RateLimitConfig> getRateLimitConfigs() { return rateLimitConfigs; }
        public void setRateLimitConfigs(List<RateLimitConfig> rateLimitConfigs) { this.rateLimitConfigs = rateLimitConfigs; }
        public List<ApprovalChain> getApprovalChains() { return approvalChains; }
        public void setApprovalChains(List<ApprovalChain> approvalChains) { this.approvalChains = approvalChains; }
        public List<AgentApproval> getAgentApprovals() { return agentApprovals; }
        public void setAgentApprovals(List<AgentApproval> agentApprovals) { this.agentApprovals = agentApprovals; }
        public List<DeploymentHistory> getDeploymentHistories() { return deploymentHistories; }
        public void setDeploymentHistories(List<DeploymentHistory> deploymentHistories) { this.deploymentHistories = deploymentHistories; }
        public List<AgentEvolutionExperience> getEvolutionExperiences() { return evolutionExperiences; }
        public void setEvolutionExperiences(List<AgentEvolutionExperience> evolutionExperiences) { this.evolutionExperiences = evolutionExperiences; }
        public List<AgentEvolutionSuggestion> getEvolutionSuggestions() { return evolutionSuggestions; }
        public void setEvolutionSuggestions(List<AgentEvolutionSuggestion> evolutionSuggestions) { this.evolutionSuggestions = evolutionSuggestions; }
        public List<AgentEvolutionReflection> getEvolutionReflections() { return evolutionReflections; }
        public void setEvolutionReflections(List<AgentEvolutionReflection> evolutionReflections) { this.evolutionReflections = evolutionReflections; }
        public List<AgentTestCase> getTestCases() { return testCases; }
        public void setTestCases(List<AgentTestCase> testCases) { this.testCases = testCases; }
        public List<AlertRule> getAlertRules() { return alertRules; }
        public void setAlertRules(List<AlertRule> alertRules) { this.alertRules = alertRules; }
        public List<PermissionMatrix> getPermissionMatrix() { return permissionMatrix; }
        public void setPermissionMatrix(List<PermissionMatrix> permissionMatrix) { this.permissionMatrix = permissionMatrix; }
        public List<DictType> getDictTypes() { return dictTypes; }
        public void setDictTypes(List<DictType> dictTypes) { this.dictTypes = dictTypes; }
    }
}
