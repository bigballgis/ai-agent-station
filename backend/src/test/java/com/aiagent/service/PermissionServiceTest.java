package com.aiagent.service;

import com.aiagent.entity.Permission;
import com.aiagent.entity.RolePermission;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.PermissionRepository;
import com.aiagent.repository.RolePermissionRepository;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PermissionService 单元测试
 * 测试权限服务的核心方法：创建权限、更新权限、删除权限、查询权限、分配权限等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("权限服务测试")
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    private static final Long TENANT_ID = 1L;
    private static final Long PERMISSION_ID = 10L;
    private static final Long ROLE_ID = 20L;

    @BeforeEach
    void setUp() {
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    // ==================== getAllPermissions 测试 ====================

    @Test
    @DisplayName("获取所有权限 - 有租户ID时按租户查询")
    void getAllPermissions_shouldReturnPermissionsByTenantId() {
        // 准备测试数据
        Permission perm1 = new Permission();
        perm1.setId(1L);
        perm1.setName("agent:read");
        Permission perm2 = new Permission();
        perm2.setId(2L);
        perm2.setName("agent:write");

        when(permissionRepository.findByTenantId(TENANT_ID)).thenReturn(Arrays.asList(perm1, perm2));

        // 执行测试
        List<Permission> result = permissionService.getAllPermissions();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(permissionRepository).findByTenantId(TENANT_ID);
        verify(permissionRepository, never()).findAll();
    }

    @Test
    @DisplayName("获取所有权限 - 无租户ID时查询全部")
    void getAllPermissions_shouldReturnAllWhenNoTenantId() {
        // 设置无租户ID
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(null);

        Permission perm = new Permission();
        perm.setId(1L);
        when(permissionRepository.findAll()).thenReturn(List.of(perm));

        // 执行测试
        List<Permission> result = permissionService.getAllPermissions();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(permissionRepository).findAll();
        verify(permissionRepository, never()).findByTenantId(any());
    }

    // ==================== getPermissionById 测试 ====================

    @Test
    @DisplayName("根据ID获取权限 - 成功返回权限")
    void getPermissionById_shouldReturnPermission() {
        // 准备测试数据
        Permission permission = new Permission();
        permission.setId(PERMISSION_ID);
        permission.setName("agent:read");

        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));

        // 执行测试
        Permission result = permissionService.getPermissionById(PERMISSION_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(PERMISSION_ID, result.getId());
        assertEquals("agent:read", result.getName());
    }

    @Test
    @DisplayName("根据ID获取权限 - 权限不存在时抛出异常")
    void getPermissionById_shouldThrowExceptionWhenNotFound() {
        // 模拟权限不存在
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> permissionService.getPermissionById(PERMISSION_ID));

        assertEquals("权限不存在", exception.getMessage());
    }

    // ==================== createPermission 测试 ====================

    @Test
    @DisplayName("创建权限 - 成功创建权限并设置租户ID")
    void createPermission_shouldCreateSuccessfully() {
        // 准备测试数据
        Permission permission = new Permission();
        permission.setName("agent:delete");
        permission.setDescription("删除Agent权限");
        permission.setResourceCode("agent");
        permission.setActionCode("delete");

        when(permissionRepository.findByNameAndTenantId("agent:delete", TENANT_ID))
                .thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> {
            Permission saved = inv.getArgument(0);
            saved.setId(PERMISSION_ID);
            return saved;
        });

        // 执行测试
        Permission result = permissionService.createPermission(permission);

        // 验证结果
        assertNotNull(result);
        assertEquals(PERMISSION_ID, result.getId());
        assertEquals(TENANT_ID, result.getTenantId());
        verify(permissionRepository).save(permission);
    }

    @Test
    @DisplayName("创建权限 - 权限名称已存在时抛出异常")
    void createPermission_shouldThrowExceptionWhenNameAlreadyExists() {
        // 准备测试数据
        Permission permission = new Permission();
        permission.setName("agent:read");

        Permission existing = new Permission();
        existing.setId(999L);

        when(permissionRepository.findByNameAndTenantId("agent:read", TENANT_ID))
                .thenReturn(Optional.of(existing));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> permissionService.createPermission(permission));

        assertEquals("权限名称已存在", exception.getMessage());
        verify(permissionRepository, never()).save(any());
    }

    // ==================== updatePermission 测试 ====================

    @Test
    @DisplayName("更新权限 - 成功更新权限信息")
    void updatePermission_shouldUpdateSuccessfully() {
        // 准备测试数据
        Permission existing = new Permission();
        existing.setId(PERMISSION_ID);
        existing.setName("old:name");
        existing.setDescription("旧描述");
        existing.setResourceCode("old_resource");
        existing.setActionCode("old_action");

        Permission details = new Permission();
        details.setName("new:name");
        details.setDescription("新描述");
        details.setResourceCode("new_resource");
        details.setActionCode("new_action");

        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(existing));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        Permission result = permissionService.updatePermission(PERMISSION_ID, details);

        // 验证结果
        assertNotNull(result);
        assertEquals("new:name", result.getName());
        assertEquals("新描述", result.getDescription());
        assertEquals("new_resource", result.getResourceCode());
        assertEquals("new_action", result.getActionCode());
        verify(permissionRepository).save(existing);
    }

    @Test
    @DisplayName("更新权限 - 权限不存在时抛出异常")
    void updatePermission_shouldThrowExceptionWhenNotFound() {
        // 模拟权限不存在
        Permission details = new Permission();
        details.setName("new:name");

        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> permissionService.updatePermission(PERMISSION_ID, details));

        assertEquals("权限不存在", exception.getMessage());
        verify(permissionRepository, never()).save(any());
    }

    // ==================== deletePermission 测试 ====================

    @Test
    @DisplayName("删除权限 - 成功删除权限")
    void deletePermission_shouldDeleteSuccessfully() {
        // 执行测试
        permissionService.deletePermission(PERMISSION_ID);

        // 验证结果
        verify(permissionRepository).deleteById(PERMISSION_ID);
    }

    // ==================== assignPermissionToRole 测试 ====================

    @Test
    @DisplayName("分配权限给角色 - 成功分配")
    void assignPermissionToRole_shouldAssignSuccessfully() {
        // 执行测试
        permissionService.assignPermissionToRole(ROLE_ID, PERMISSION_ID);

        // 验证结果
        verify(rolePermissionRepository).save(argThat(rp -> {
            assertEquals(ROLE_ID, rp.getRoleId());
            assertEquals(PERMISSION_ID, rp.getPermissionId());
            return true;
        }));
    }

    // ==================== removePermissionFromRole 测试 ====================

    @Test
    @DisplayName("移除角色权限 - 成功移除")
    void removePermissionFromRole_shouldRemoveSuccessfully() {
        // 执行测试
        permissionService.removePermissionFromRole(ROLE_ID, PERMISSION_ID);

        // 验证结果
        verify(rolePermissionRepository).deleteByRoleIdAndPermissionId(ROLE_ID, PERMISSION_ID);
    }

    // ==================== getRolePermissions 测试 ====================

    @Test
    @DisplayName("获取角色权限列表 - 成功返回权限列表")
    void getRolePermissions_shouldReturnRolePermissions() {
        // 准备测试数据
        RolePermission rp1 = new RolePermission();
        rp1.setId(1L);
        rp1.setRoleId(ROLE_ID);
        rp1.setPermissionId(10L);
        RolePermission rp2 = new RolePermission();
        rp2.setId(2L);
        rp2.setRoleId(ROLE_ID);
        rp2.setPermissionId(20L);

        when(rolePermissionRepository.findByRoleId(ROLE_ID)).thenReturn(Arrays.asList(rp1, rp2));

        // 执行测试
        List<RolePermission> result = permissionService.getRolePermissions(ROLE_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(rolePermissionRepository).findByRoleId(ROLE_ID);
    }
}
