package com.aiagent.service;

import com.aiagent.entity.Tenant;
import com.aiagent.entity.User;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.*;
import com.aiagent.security.ApiKeyService;
import com.aiagent.util.CryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TenantService 单元测试
 * 覆盖租户创建、更新、删除（软删除）、重新激活、API Key 重新生成等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("租户服务测试")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private CryptoUtils cryptoUtils;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private TenantService tenantService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tenantService, "schemaPrefix", "t_");
        ReflectionTestUtils.setField(tenantService, "defaultAdminPassword", "Admin@123456");
    }

    // ==================== getAllTenants ====================

    @Test
    @DisplayName("获取所有租户 - 成功返回租户列表")
    void getAllTenants_shouldReturnAllTenants() {
        Tenant tenant1 = new Tenant();
        tenant1.setId(1L);
        tenant1.setName("租户A");
        Tenant tenant2 = new Tenant();
        tenant2.setId(2L);
        tenant2.setName("租户B");

        when(tenantRepository.findAll()).thenReturn(Arrays.asList(tenant1, tenant2));

        List<Tenant> result = tenantService.getAllTenants();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(tenantRepository).findAll();
    }

    // ==================== getTenantById ====================

    @Test
    @DisplayName("根据ID获取租户 - 成功返回租户")
    void getTenantById_shouldReturnTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("测试租户");

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantById(TENANT_ID);

        assertNotNull(result);
        assertEquals(TENANT_ID, result.getId());
    }

    @Test
    @DisplayName("根据ID获取租户 - 租户不存在时抛出异常")
    void getTenantById_shouldThrowExceptionWhenNotFound() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> tenantService.getTenantById(TENANT_ID));

        assertEquals("租户不存在", exception.getMessage());
    }

    // ==================== createTenant ====================

    @Nested
    @DisplayName("createTenant 创建租户")
    class CreateTenantTests {

        @Test
        @DisplayName("有效数据 - 成功创建租户并生成API密钥")
        void createTenant_withValidData_shouldSucceed() {
            Tenant tenant = new Tenant();
            tenant.setName("新租户");
            tenant.setDescription("新租户描述");

            when(tenantRepository.findByName("新租户")).thenReturn(Optional.empty());
            when(cryptoUtils.encrypt(anyString())).thenReturn("encrypted-key", "encrypted-secret");
            when(cryptoUtils.decrypt(anyString())).thenReturn("plain-key", "plain-secret");
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
                Tenant saved = inv.getArgument(0);
                if (saved.getId() == null) {
                    saved.setId(TENANT_ID);
                }
                return saved;
            });
            when(roleRepository.findByNameAndTenantId(anyString(), eq(TENANT_ID)))
                    .thenReturn(Optional.empty());
            when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(permissionRepository.findByNameAndTenantId(anyString(), eq(TENANT_ID)))
                    .thenReturn(Optional.empty());
            when(permissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByUsernameAndTenantId(anyString(), eq(TENANT_ID)))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                if (u.getId() == null) u.setId(1L);
                return u;
            });
            when(roleRepository.findByNameAndTenantId("ROLE_ADMIN", TENANT_ID))
                    .thenReturn(Optional.empty());

            Tenant result = tenantService.createTenant(tenant);

            assertNotNull(result);
            assertEquals(TENANT_ID, result.getId());
            assertNotNull(result.getApiKey());
            assertTrue(result.getIsActive());
            assertEquals("t_" + TENANT_ID, result.getSchemaName());
            verify(apiKeyService).saveApiKey(anyString(), eq(TENANT_ID), eq(31536000L));
        }

        @Test
        @DisplayName("租户名称已存在 - 抛出异常")
        void createTenant_withDuplicateName_shouldThrowException() {
            Tenant tenant = new Tenant();
            tenant.setName("已存在租户");

            Tenant existingTenant = new Tenant();
            existingTenant.setId(999L);

            when(tenantRepository.findByName("已存在租户")).thenReturn(Optional.of(existingTenant));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> tenantService.createTenant(tenant));

            assertEquals("租户名称已存在", exception.getMessage());
            verify(tenantRepository, never()).save(any());
        }
    }

    // ==================== updateTenant ====================

    @Nested
    @DisplayName("updateTenant 更新租户")
    class UpdateTenantTests {

        @Test
        @DisplayName("更新租户 - 成功更新名称和描述")
        void updateTenant_shouldUpdateSuccessfully() {
            Tenant existingTenant = new Tenant();
            existingTenant.setId(TENANT_ID);
            existingTenant.setName("旧名称");
            existingTenant.setDescription("旧描述");

            Tenant tenantDetails = new Tenant();
            tenantDetails.setName("新名称");
            tenantDetails.setDescription("新描述");

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(existingTenant));
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

            Tenant result = tenantService.updateTenant(TENANT_ID, tenantDetails);

            assertNotNull(result);
            assertEquals("新名称", result.getName());
            assertEquals("新描述", result.getDescription());
            verify(tenantRepository).save(existingTenant);
        }

        @Test
        @DisplayName("更新租户 - 租户不存在时抛出异常")
        void updateTenant_notFound_shouldThrowException() {
            Tenant tenantDetails = new Tenant();
            tenantDetails.setName("新名称");

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> tenantService.updateTenant(TENANT_ID, tenantDetails));

            verify(tenantRepository, never()).save(any());
        }
    }

    // ==================== deleteTenant ====================

    @Nested
    @DisplayName("deleteTenant 删除（停用）租户")
    class DeleteTenantTests {

        @Test
        @DisplayName("停用租户 - 成功软删除并撤销API Key")
        void deleteTenant_shouldSoftDeleteAndRevokeApiKey() {
            Tenant tenant = new Tenant();
            tenant.setId(TENANT_ID);
            tenant.setName("待停用租户");
            tenant.setIsActive(true);
            tenant.setApiKey("encrypted-api-key");

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(cryptoUtils.decrypt("encrypted-api-key")).thenReturn("plain-api-key");
            when(userRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

            tenantService.deleteTenant(TENANT_ID);

            assertFalse(tenant.getIsActive());
            verify(apiKeyService).revokeApiKey("plain-api-key");
            verify(tenantRepository).save(tenant);
        }

        @Test
        @DisplayName("停用租户 - 使所有用户会话失效并禁用用户")
        void deleteTenant_shouldInvalidateSessionsAndDisableUsers() {
            Tenant tenant = new Tenant();
            tenant.setId(TENANT_ID);
            tenant.setName("待停用租户");
            tenant.setIsActive(true);

            User user1 = new User();
            user1.setId(10L);
            user1.setIsActive(true);
            User user2 = new User();
            user2.setId(20L);
            user2.setIsActive(true);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(userRepository.findByTenantId(TENANT_ID)).thenReturn(Arrays.asList(user1, user2));
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

            tenantService.deleteTenant(TENANT_ID);

            verify(sessionService).invalidateAllUserSessions(10L);
            verify(sessionService).invalidateAllUserSessions(20L);
            assertFalse(user1.getIsActive());
            assertFalse(user2.getIsActive());
            verify(userRepository).saveAll(Arrays.asList(user1, user2));
        }

        @Test
        @DisplayName("停用租户 - 租户不存在时抛出异常")
        void deleteTenant_notFound_shouldThrowException() {
            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> tenantService.deleteTenant(TENANT_ID));

            assertEquals("租户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("停用租户 - 租户已停用时抛出异常")
        void deleteTenant_alreadyInactive_shouldThrowException() {
            Tenant tenant = new Tenant();
            tenant.setId(TENANT_ID);
            tenant.setName("已停用租户");
            tenant.setIsActive(false);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> tenantService.deleteTenant(TENANT_ID));

            assertEquals("租户已处于停用状态", exception.getMessage());
        }
    }

    // ==================== reactivateTenant ====================

    @Nested
    @DisplayName("reactivateTenant 重新激活租户")
    class ReactivateTenantTests {

        @Test
        @DisplayName("重新激活租户 - 成功激活并生成新API Key")
        void reactivateTenant_shouldActivateAndGenerateNewApiKey() {
            Tenant tenant = new Tenant();
            tenant.setId(TENANT_ID);
            tenant.setName("已停用租户");
            tenant.setIsActive(false);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(cryptoUtils.encrypt(anyString())).thenReturn("new-encrypted-key", "new-encrypted-secret");
            when(cryptoUtils.decrypt(anyString())).thenReturn("new-plain-key", "new-plain-secret");
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

            Tenant result = tenantService.reactivateTenant(TENANT_ID);

            assertNotNull(result);
            assertTrue(result.getIsActive());
            assertNotNull(result.getApiKey());
            verify(apiKeyService).saveApiKey(anyString(), eq(TENANT_ID), eq(31536000L));
        }

        @Test
        @DisplayName("重新激活租户 - 租户不存在时抛出异常")
        void reactivateTenant_notFound_shouldThrowException() {
            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> tenantService.reactivateTenant(TENANT_ID));
        }

        @Test
        @DisplayName("重新激活租户 - 租户已激活时抛出异常")
        void reactivateTenant_alreadyActive_shouldThrowException() {
            Tenant tenant = new Tenant();
            tenant.setId(TENANT_ID);
            tenant.setName("已激活租户");
            tenant.setIsActive(true);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> tenantService.reactivateTenant(TENANT_ID));

            assertEquals("租户已处于激活状态", exception.getMessage());
        }
    }

    // ==================== regenerateApiKey ====================

    @Nested
    @DisplayName("regenerateApiKey 重新生成API密钥")
    class RegenerateApiKeyTests {

        @Test
        @DisplayName("重新生成API密钥 - 成功撤销旧密钥并生成新密钥")
        void regenerateApiKey_shouldRevokeOldAndGenerateNew() {
            Tenant tenant = new Tenant();
            tenant.setId(TENANT_ID);
            tenant.setApiKey("old-encrypted-key");
            tenant.setApiSecret("old-encrypted-secret");

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(cryptoUtils.decrypt("old-encrypted-key")).thenReturn("old-plain-key");
            when(cryptoUtils.encrypt(anyString())).thenReturn("new-encrypted-key", "new-encrypted-secret");
            when(cryptoUtils.decrypt("new-encrypted-key")).thenReturn("new-plain-key");
            when(cryptoUtils.decrypt("new-encrypted-secret")).thenReturn("new-plain-secret");
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

            Tenant result = tenantService.regenerateApiKey(TENANT_ID);

            assertNotNull(result);
            assertNotEquals("old-encrypted-key", result.getApiKey());
            verify(apiKeyService).revokeApiKey("old-plain-key");
            verify(apiKeyService).saveApiKey(anyString(), eq(TENANT_ID), eq(31536000L));
        }

        @Test
        @DisplayName("重新生成API密钥 - 租户不存在时抛出异常")
        void regenerateApiKey_notFound_shouldThrowException() {
            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> tenantService.regenerateApiKey(TENANT_ID));
        }

        @Test
        @DisplayName("重新生成API密钥 - apiKey 为 null 时跳过撤销")
        void regenerateApiKey_nullApiKey_shouldSkipRevoke() {
            Tenant tenant = new Tenant();
            tenant.setId(TENANT_ID);
            tenant.setApiKey(null);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(cryptoUtils.encrypt(anyString())).thenReturn("new-encrypted-key", "new-encrypted-secret");
            when(cryptoUtils.decrypt(anyString())).thenReturn("new-plain-key", "new-plain-secret");
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

            Tenant result = tenantService.regenerateApiKey(TENANT_ID);

            assertNotNull(result);
            verify(apiKeyService, never()).revokeApiKey(anyString());
            verify(apiKeyService).saveApiKey(anyString(), eq(TENANT_ID), eq(31536000L));
        }
    }
}
