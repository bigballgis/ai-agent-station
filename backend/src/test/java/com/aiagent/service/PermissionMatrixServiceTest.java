package com.aiagent.service;

import com.aiagent.entity.Permission;
import com.aiagent.entity.RolePermission;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.PermissionRepository;
import com.aiagent.repository.RolePermissionRepository;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PermissionMatrixService 单元测试
 * 测试权限检查、授权、撤销等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("权限矩阵服务测试")
class PermissionMatrixServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private Permission testPermission;
    private RolePermission testRolePermission;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        testPermission = new Permission();
        testPermission.setId(1L);
        testPermission.setName("agent:read");
        testPermission.setDescription("查看Agent");
        testPermission.setResourceCode("agent");
        testPermission.setActionCode("read");

        testRolePermission = new RolePermission();
        testRolePermission.setId(1L);
        testRolePermission.setRoleId(1L);
        testRolePermission.setPermissionId(1L);

        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(100L);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    @Test
    @DisplayName("检查权限 - 有权限（允许）")
    void testCheckPermission_Allowed() {
        when(rolePermissionRepository.findByRoleId(1L))
                .thenReturn(List.of(testRolePermission));

        List<RolePermission> result = permissionService.getRolePermissions(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1L, result.get(0).getPermissionId());
    }

    @Test
    @DisplayName("检查权限 - 无权限（拒绝）")
    void testCheckPermission_Denied() {
        when(rolePermissionRepository.findByRoleId(999L))
                .thenReturn(Collections.emptyList());

        List<RolePermission> result = permissionService.getRolePermissions(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("授权 - 给角色分配权限")
    void testGrantPermission() {
        when(rolePermissionRepository.save(any(RolePermission.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        permissionService.assignPermissionToRole(1L, 1L);

        verify(rolePermissionRepository).save(argThat(rp ->
                rp.getRoleId() == 1L && rp.getPermissionId() == 1L
        ));
    }

    @Test
    @DisplayName("撤销权限 - 从角色移除权限")
    void testRevokePermission() {
        doNothing().when(rolePermissionRepository).deleteByRoleIdAndPermissionId(1L, 1L);

        permissionService.removePermissionFromRole(1L, 1L);

        verify(rolePermissionRepository).deleteByRoleIdAndPermissionId(1L, 1L);
    }

    @Test
    @DisplayName("获取所有权限 - 成功")
    void testGetAllPermissions() {
        Permission perm2 = new Permission();
        perm2.setId(2L);
        perm2.setName("agent:write");
        when(permissionRepository.findByTenantId(100L))
                .thenReturn(Arrays.asList(testPermission, perm2));

        List<Permission> result = permissionService.getAllPermissions();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("根据ID获取权限 - 成功")
    void testGetPermissionById() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(testPermission));

        Permission result = permissionService.getPermissionById(1L);

        assertNotNull(result);
        assertEquals("agent:read", result.getName());
    }

    @Test
    @DisplayName("根据ID获取权限 - 不存在抛出异常")
    void testGetPermissionById_NotFound() {
        when(permissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> permissionService.getPermissionById(999L));
    }

    @Test
    @DisplayName("创建权限 - 成功")
    void testCreatePermission() {
        when(permissionRepository.findByNameAndTenantId("new:perm", 100L))
                .thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> {
            Permission p = inv.getArgument(0);
            p.setId(3L);
            return p;
        });

        Permission newPerm = new Permission();
        newPerm.setName("new:perm");
        newPerm.setDescription("新权限");

        Permission result = permissionService.createPermission(newPerm);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals(100L, result.getTenantId());
    }

    @Test
    @DisplayName("创建权限 - 名称已存在抛出异常")
    void testCreatePermission_DuplicateName() {
        when(permissionRepository.findByNameAndTenantId("agent:read", 100L))
                .thenReturn(Optional.of(testPermission));

        Permission dupPerm = new Permission();
        dupPerm.setName("agent:read");

        assertThrows(BusinessException.class, () -> permissionService.createPermission(dupPerm));
    }

    @Test
    @DisplayName("更新权限 - 成功")
    void testUpdatePermission() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        Permission updateData = new Permission();
        updateData.setName("agent:read_updated");
        updateData.setDescription("更新后的描述");

        Permission result = permissionService.updatePermission(1L, updateData);

        assertNotNull(result);
        assertEquals("agent:read_updated", result.getName());
    }

    @Test
    @DisplayName("删除权限 - 成功")
    void testDeletePermission() {
        doNothing().when(permissionRepository).deleteById(1L);

        permissionService.deletePermission(1L);

        verify(permissionRepository).deleteById(1L);
    }
}
