package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Permission;
import com.aiagent.entity.Role;
import com.aiagent.entity.Tenant;
import com.aiagent.entity.User;
import com.aiagent.entity.UserRole;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.TenantRepository;
import com.aiagent.repository.UserRepository;
import com.aiagent.repository.UserRoleRepository;
import com.aiagent.repository.RoleRepository;
import com.aiagent.repository.PermissionRepository;
import com.aiagent.security.ApiKeyService;
import com.aiagent.security.annotation.Auditable;
import com.aiagent.tenant.TenantContextHolder;
import com.aiagent.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ApiKeyService apiKeyService;
    private final CryptoUtils cryptoUtils;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Value("${ai-agent.tenant.schema-prefix:t_}")
    private String schemaPrefix;

    @Value("${ai-agent.tenant.default-admin-password:Admin@123456}")
    private String defaultAdminPassword;

    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        // 解密 API Key 和 Secret 返回给前端
        tenants.forEach(this::decryptTenantKeys);
        return tenants;
    }

    public Tenant getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.TENANT_NOT_FOUND));
        decryptTenantKeys(tenant);
        return tenant;
    }

    /**
     * 创建租户（完整上线流程）
     * 1. 验证租户名称唯一性
     * 2. 生成并加密 API Key/Secret
     * 3. 保存租户记录
     * 4. 创建默认角色和权限
     * 5. 创建管理员用户并分配角色
     * 6. 保存 API Key 到 Redis
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "tenant", description = "创建租户")
    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.findByName(tenant.getName()).isPresent()) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "租户名称已存在");
        }

        String apiKey = UUID.randomUUID().toString();
        String apiSecret = UUID.randomUUID().toString();

        // 加密存储 API Key 和 Secret
        tenant.setApiKey(cryptoUtils.encrypt(apiKey));
        tenant.setApiSecret(cryptoUtils.encrypt(apiSecret));
        tenant.setIsActive(true);

        Tenant savedTenant = tenantRepository.save(tenant);
        savedTenant.setSchemaName(schemaPrefix + savedTenant.getId());
        tenantRepository.save(savedTenant);

        apiKeyService.saveApiKey(apiKey, savedTenant.getId(), 31536000L);

        // 初始化默认角色和权限
        initializeDefaultRolesAndPermissions(savedTenant.getId());

        // 创建管理员用户
        initializeAdminUser(savedTenant.getId());

        log.info("租户创建成功: id={}, name={}", savedTenant.getId(), savedTenant.getName());

        // 返回解密后的数据
        decryptTenantKeys(savedTenant);
        return savedTenant;
    }

    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "tenant", description = "更新租户")
    public Tenant updateTenant(Long id, Tenant tenantDetails) {
        Tenant tenant = getTenantById(id);
        tenant.setName(tenantDetails.getName());
        tenant.setDescription(tenantDetails.getDescription());
        return tenantRepository.save(tenant);
    }

    /**
     * 停用租户（完整下线流程）
     * 1. 软删除（设置 isActive=false）
     * 2. 撤销 API Key
     * 3. 使所有用户会话失效
     * 4. 禁用所有用户
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "tenant", description = "停用租户")
    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.TENANT_NOT_FOUND));

        if (!tenant.getIsActive()) {
            throw new BusinessException("租户已处于停用状态");
        }

        // 1. 软删除
        tenant.setIsActive(false);
        tenantRepository.save(tenant);

        // 2. 撤销 API Key
        if (tenant.getApiKey() != null) {
            try {
                String oldApiKey = cryptoUtils.decrypt(tenant.getApiKey());
                apiKeyService.revokeApiKey(oldApiKey);
                log.info("已撤销租户 API Key: tenantId={}", id);
            } catch (Exception e) {
                log.warn("撤销租户 API Key 失败: tenantId={}, error={}", id, e.getMessage());
            }
        }

        // 3. 使所有用户会话失效
        List<User> tenantUsers = userRepository.findByTenantId(id);
        for (User user : tenantUsers) {
            try {
                sessionService.invalidateAllUserSessions(user.getId());
            } catch (Exception e) {
                log.warn("使用户会话失效失败: userId={}, error={}", user.getId(), e.getMessage());
            }
        }

        // 4. 禁用所有用户
        for (User user : tenantUsers) {
            user.setIsActive(false);
        }
        userRepository.saveAll(tenantUsers);

        log.info("租户停用完成: id={}, name={}, 受影响用户数={}", id, tenant.getName(), tenantUsers.size());
    }

    /**
     * 重新激活租户
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(tableName = "tenant", description = "重新激活租户")
    public Tenant reactivateTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.TENANT_NOT_FOUND));

        if (tenant.getIsActive()) {
            throw new BusinessException("租户已处于激活状态");
        }

        tenant.setIsActive(true);
        Tenant savedTenant = tenantRepository.save(tenant);

        // 重新生成 API Key
        String newApiKey = UUID.randomUUID().toString();
        String newApiSecret = UUID.randomUUID().toString();
        tenant.setApiKey(cryptoUtils.encrypt(newApiKey));
        tenant.setApiSecret(cryptoUtils.encrypt(newApiSecret));
        apiKeyService.saveApiKey(newApiKey, savedTenant.getId(), 31536000L);

        log.info("租户重新激活: id={}, name={}", id, savedTenant.getName());

        decryptTenantKeys(savedTenant);
        return savedTenant;
    }

    @Transactional(rollbackFor = Exception.class)
    public Tenant regenerateApiKey(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.TENANT_NOT_FOUND));

        if (tenant.getApiKey() != null) {
            // 解密旧的 API Key 用于撤销
            String oldApiKey = cryptoUtils.decrypt(tenant.getApiKey());
            apiKeyService.revokeApiKey(oldApiKey);
        }

        String newApiKey = UUID.randomUUID().toString();
        String newApiSecret = UUID.randomUUID().toString();

        // 加密存储新的 API Key 和 Secret
        tenant.setApiKey(cryptoUtils.encrypt(newApiKey));
        tenant.setApiSecret(cryptoUtils.encrypt(newApiSecret));

        apiKeyService.saveApiKey(newApiKey, tenant.getId(), 31536000L);

        Tenant savedTenant = tenantRepository.save(tenant);
        decryptTenantKeys(savedTenant);
        return savedTenant;
    }

    // ==================== 私有方法 ====================

    /**
     * 初始化默认角色和权限
     */
    private void initializeDefaultRolesAndPermissions(Long tenantId) {
        // 创建默认角色
        Role adminRole = createRoleIfNotExists(tenantId, "ROLE_ADMIN", "租户管理员 - 拥有所有权限");
        Role userRole = createRoleIfNotExists(tenantId, "ROLE_USER", "普通用户 - 基础使用权限");

        // 创建默认权限
        createPermissionIfNotExists(tenantId, "agent:view", "查看Agent", "agent", "view");
        createPermissionIfNotExists(tenantId, "agent:create", "创建Agent", "agent", "create");
        createPermissionIfNotExists(tenantId, "agent:update", "更新Agent", "agent", "update");
        createPermissionIfNotExists(tenantId, "agent:delete", "删除Agent", "agent", "delete");
        createPermissionIfNotExists(tenantId, "workflow:view", "查看工作流", "workflow", "view");
        createPermissionIfNotExists(tenantId, "workflow:manage", "管理工作流", "workflow", "manage");
        createPermissionIfNotExists(tenantId, "file:upload", "上传文件", "file", "upload");
        createPermissionIfNotExists(tenantId, "file:view", "查看文件", "file", "view");
        createPermissionIfNotExists(tenantId, "file:delete", "删除文件", "file", "delete");
        createPermissionIfNotExists(tenantId, "tenant:read", "查看租户信息", "tenant", "read");

        log.info("默认角色和权限初始化完成: tenantId={}", tenantId);
    }

    /**
     * 创建管理员用户
     */
    private void initializeAdminUser(Long tenantId) {
        String adminUsername = "admin_t" + tenantId;

        if (userRepository.findByUsernameAndTenantId(adminUsername, tenantId).isPresent()) {
            log.info("管理员用户已存在，跳过创建: tenantId={}", tenantId);
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
        admin.setTenantId(tenantId);
        admin.setIsActive(true);
        admin.setEmail("admin@tenant" + tenantId + ".local");
        userRepository.save(admin);

        // 分配 ROLE_ADMIN
        Role adminRole = roleRepository.findByNameAndTenantId("ROLE_ADMIN", tenantId).orElse(null);
        if (adminRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(adminRole.getId());
            userRoleRepository.save(userRole);
        }

        log.info("管理员用户创建完成: tenantId={}, username={}", tenantId, adminUsername);
    }

    private Role createRoleIfNotExists(Long tenantId, String name, String description) {
        return roleRepository.findByNameAndTenantId(name, tenantId)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setTenantId(tenantId);
                    role.setName(name);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    private Permission createPermissionIfNotExists(Long tenantId, String name, String description,
                                                    String resourceCode, String actionCode) {
        return permissionRepository.findByNameAndTenantId(name, tenantId)
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setTenantId(tenantId);
                    permission.setName(name);
                    permission.setDescription(description);
                    permission.setResourceCode(resourceCode);
                    permission.setActionCode(actionCode);
                    return permissionRepository.save(permission);
                });
    }

    /**
     * 解密租户的 API Key 和 Secret（用于返回给前端）
     */
    private void decryptTenantKeys(Tenant tenant) {
        if (tenant.getApiKey() != null) {
            tenant.setApiKey(cryptoUtils.decrypt(tenant.getApiKey()));
        }
        if (tenant.getApiSecret() != null) {
            tenant.setApiSecret(cryptoUtils.decrypt(tenant.getApiSecret()));
        }
    }
}
