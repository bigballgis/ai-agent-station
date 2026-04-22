package com.aiagent.service;

import com.aiagent.entity.Tenant;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.TenantRepository;
import com.aiagent.security.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TenantService 单元测试
 * 测试租户服务的核心方法：创建租户、更新租户、删除租户、查询租户等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("租户服务测试")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private TenantService tenantService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        // 通过反射注入 @Value 字段
        ReflectionTestUtils.setField(tenantService, "schemaPrefix", "t_");
    }

    // ==================== getAllTenants 测试 ====================

    @Test
    @DisplayName("获取所有租户 - 成功返回租户列表")
    void getAllTenants_shouldReturnAllTenants() {
        // 准备测试数据
        Tenant tenant1 = new Tenant();
        tenant1.setId(1L);
        tenant1.setName("租户A");
        Tenant tenant2 = new Tenant();
        tenant2.setId(2L);
        tenant2.setName("租户B");

        when(tenantRepository.findAll()).thenReturn(Arrays.asList(tenant1, tenant2));

        // 执行测试
        List<Tenant> result = tenantService.getAllTenants();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(tenantRepository).findAll();
    }

    // ==================== getTenantById 测试 ====================

    @Test
    @DisplayName("根据ID获取租户 - 成功返回租户")
    void getTenantById_shouldReturnTenant() {
        // 准备测试数据
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("测试租户");

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        // 执行测试
        Tenant result = tenantService.getTenantById(TENANT_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getId());
        assertEquals("测试租户", result.getName());
    }

    @Test
    @DisplayName("根据ID获取租户 - 租户不存在时抛出异常")
    void getTenantById_shouldThrowExceptionWhenNotFound() {
        // 模拟租户不存在
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tenantService.getTenantById(TENANT_ID));

        assertEquals("租户不存在", exception.getMessage());
    }

    // ==================== createTenant 测试 ====================

    @Test
    @DisplayName("创建租户 - 成功创建租户并生成API密钥")
    void createTenant_shouldCreateTenantSuccessfully() {
        // 准备测试数据
        Tenant tenant = new Tenant();
        tenant.setName("新租户");
        tenant.setDescription("新租户描述");

        when(tenantRepository.findByName("新租户")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(TENANT_ID);
            }
            return saved;
        });

        // 执行测试
        Tenant result = tenantService.createTenant(tenant);

        // 验证结果
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getId());
        assertNotNull(result.getApiKey());
        assertNotNull(result.getApiSecret());
        assertTrue(result.getIsActive());
        assertEquals("t_" + TENANT_ID, result.getSchemaName());
        verify(apiKeyService).saveApiKey(anyString(), eq(TENANT_ID), eq(31536000L));
        // save被调用两次：第一次保存获取ID，第二次更新schemaName
        verify(tenantRepository, times(2)).save(any(Tenant.class));
    }

    @Test
    @DisplayName("创建租户 - 租户名称已存在时抛出异常")
    void createTenant_shouldThrowExceptionWhenNameAlreadyExists() {
        // 准备测试数据
        Tenant tenant = new Tenant();
        tenant.setName("已存在租户");

        Tenant existingTenant = new Tenant();
        existingTenant.setId(999L);

        when(tenantRepository.findByName("已存在租户")).thenReturn(Optional.of(existingTenant));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tenantService.createTenant(tenant));

        assertEquals("租户名称已存在", exception.getMessage());
        verify(tenantRepository, never()).save(any());
    }

    // ==================== updateTenant 测试 ====================

    @Test
    @DisplayName("更新租户 - 成功更新租户信息")
    void updateTenant_shouldUpdateTenantSuccessfully() {
        // 准备测试数据
        Tenant existingTenant = new Tenant();
        existingTenant.setId(TENANT_ID);
        existingTenant.setName("旧名称");
        existingTenant.setDescription("旧描述");

        Tenant tenantDetails = new Tenant();
        tenantDetails.setName("新名称");
        tenantDetails.setDescription("新描述");

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        Tenant result = tenantService.updateTenant(TENANT_ID, tenantDetails);

        // 验证结果
        assertNotNull(result);
        assertEquals("新名称", result.getName());
        assertEquals("新描述", result.getDescription());
        verify(tenantRepository).save(existingTenant);
    }

    @Test
    @DisplayName("更新租户 - 租户不存在时抛出异常")
    void updateTenant_shouldThrowExceptionWhenNotFound() {
        // 模拟租户不存在
        Tenant tenantDetails = new Tenant();
        tenantDetails.setName("新名称");

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tenantService.updateTenant(TENANT_ID, tenantDetails));

        assertEquals("租户不存在", exception.getMessage());
        verify(tenantRepository, never()).save(any());
    }

    // ==================== deleteTenant 测试 ====================

    @Test
    @DisplayName("删除租户 - 成功软删除租户（设置isActive为false）")
    void deleteTenant_shouldSoftDeleteTenant() {
        // 准备测试数据
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("待删除租户");
        tenant.setIsActive(true);

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        tenantService.deleteTenant(TENANT_ID);

        // 验证结果 - 软删除，设置isActive为false
        assertFalse(tenant.getIsActive());
        verify(tenantRepository).save(tenant);
    }

    @Test
    @DisplayName("删除租户 - 租户不存在时抛出异常")
    void deleteTenant_shouldThrowExceptionWhenNotFound() {
        // 模拟租户不存在
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tenantService.deleteTenant(TENANT_ID));

        assertEquals("租户不存在", exception.getMessage());
    }

    // ==================== regenerateApiKey 测试 ====================

    @Test
    @DisplayName("重新生成API密钥 - 成功重新生成")
    void regenerateApiKey_shouldRegenerateSuccessfully() {
        // 准备测试数据
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setApiKey("old-api-key");
        tenant.setApiSecret("old-api-secret");

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        Tenant result = tenantService.regenerateApiKey(TENANT_ID);

        // 验证结果
        assertNotNull(result);
        assertNotEquals("old-api-key", result.getApiKey());
        assertNotEquals("old-api-secret", result.getApiSecret());
        verify(apiKeyService).revokeApiKey("old-api-key");
        verify(apiKeyService).saveApiKey(anyString(), eq(TENANT_ID), eq(31536000L));
    }

    @Test
    @DisplayName("重新生成API密钥 - 租户不存在时抛出异常")
    void regenerateApiKey_shouldThrowExceptionWhenNotFound() {
        // 模拟租户不存在
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> tenantService.regenerateApiKey(TENANT_ID));

        assertEquals("租户不存在", exception.getMessage());
    }
}
