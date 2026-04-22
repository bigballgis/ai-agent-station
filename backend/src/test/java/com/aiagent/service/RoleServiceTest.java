package com.aiagent.service;

import com.aiagent.entity.Role;
import com.aiagent.entity.UserRole;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.RoleRepository;
import com.aiagent.repository.UserRoleRepository;
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
 * RoleService 单元测试
 * 测试角色服务的核心方法：创建角色、更新角色、删除角色、查询角色、分配角色等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色服务测试")
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private RoleService roleService;

    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    private static final Long TENANT_ID = 1L;
    private static final Long ROLE_ID = 10L;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    // ==================== getAllRoles 测试 ====================

    @Test
    @DisplayName("获取所有角色 - 有租户ID时按租户查询")
    void getAllRoles_shouldReturnRolesByTenantId() {
        // 准备测试数据
        Role role1 = new Role();
        role1.setId(1L);
        role1.setName("ADMIN");
        Role role2 = new Role();
        role2.setId(2L);
        role2.setName("USER");

        when(roleRepository.findByTenantId(TENANT_ID)).thenReturn(Arrays.asList(role1, role2));

        // 执行测试
        List<Role> result = roleService.getAllRoles();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(roleRepository).findByTenantId(TENANT_ID);
        verify(roleRepository, never()).findAll();
    }

    @Test
    @DisplayName("获取所有角色 - 无租户ID时查询全部")
    void getAllRoles_shouldReturnAllRolesWhenNoTenantId() {
        // 设置无租户ID
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(null);

        Role role = new Role();
        role.setId(1L);
        when(roleRepository.findAll()).thenReturn(List.of(role));

        // 执行测试
        List<Role> result = roleService.getAllRoles();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(roleRepository).findAll();
        verify(roleRepository, never()).findByTenantId(any());
    }

    // ==================== getRoleById 测试 ====================

    @Test
    @DisplayName("根据ID获取角色 - 成功返回角色")
    void getRoleById_shouldReturnRole() {
        // 准备测试数据
        Role role = new Role();
        role.setId(ROLE_ID);
        role.setName("ADMIN");

        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));

        // 执行测试
        Role result = roleService.getRoleById(ROLE_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(ROLE_ID, result.getId());
        assertEquals("ADMIN", result.getName());
    }

    @Test
    @DisplayName("根据ID获取角色 - 角色不存在时抛出异常")
    void getRoleById_shouldThrowExceptionWhenNotFound() {
        // 模拟角色不存在
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> roleService.getRoleById(ROLE_ID));

        assertEquals("角色不存在", exception.getMessage());
    }

    // ==================== createRole 测试 ====================

    @Test
    @DisplayName("创建角色 - 成功创建角色并设置租户ID")
    void createRole_shouldCreateRoleSuccessfully() {
        // 准备测试数据
        Role role = new Role();
        role.setName("NEW_ROLE");
        role.setDescription("新角色描述");

        when(roleRepository.findByNameAndTenantId("NEW_ROLE", TENANT_ID)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
            Role saved = inv.getArgument(0);
            saved.setId(ROLE_ID);
            return saved;
        });

        // 执行测试
        Role result = roleService.createRole(role);

        // 验证结果
        assertNotNull(result);
        assertEquals(ROLE_ID, result.getId());
        assertEquals(TENANT_ID, result.getTenantId());
        verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("创建角色 - 角色名称已存在时抛出异常")
    void createRole_shouldThrowExceptionWhenNameAlreadyExists() {
        // 准备测试数据
        Role role = new Role();
        role.setName("DUPLICATE_ROLE");

        Role existingRole = new Role();
        existingRole.setId(999L);

        when(roleRepository.findByNameAndTenantId("DUPLICATE_ROLE", TENANT_ID))
                .thenReturn(Optional.of(existingRole));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> roleService.createRole(role));

        assertEquals("角色名称已存在", exception.getMessage());
        verify(roleRepository, never()).save(any());
    }

    // ==================== updateRole 测试 ====================

    @Test
    @DisplayName("更新角色 - 成功更新角色信息")
    void updateRole_shouldUpdateRoleSuccessfully() {
        // 准备测试数据
        Role existingRole = new Role();
        existingRole.setId(ROLE_ID);
        existingRole.setName("OLD_NAME");
        existingRole.setDescription("旧描述");

        Role roleDetails = new Role();
        roleDetails.setName("NEW_NAME");
        roleDetails.setDescription("新描述");

        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(existingRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行测试
        Role result = roleService.updateRole(ROLE_ID, roleDetails);

        // 验证结果
        assertNotNull(result);
        assertEquals("NEW_NAME", result.getName());
        assertEquals("新描述", result.getDescription());
        verify(roleRepository).save(existingRole);
    }

    @Test
    @DisplayName("更新角色 - 角色不存在时抛出异常")
    void updateRole_shouldThrowExceptionWhenNotFound() {
        // 模拟角色不存在
        Role roleDetails = new Role();
        roleDetails.setName("NEW_NAME");

        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> roleService.updateRole(ROLE_ID, roleDetails));

        assertEquals("角色不存在", exception.getMessage());
        verify(roleRepository, never()).save(any());
    }

    // ==================== deleteRole 测试 ====================

    @Test
    @DisplayName("删除角色 - 成功删除角色")
    void deleteRole_shouldDeleteRoleSuccessfully() {
        // 执行测试
        roleService.deleteRole(ROLE_ID);

        // 验证结果
        verify(roleRepository).deleteById(ROLE_ID);
    }

    // ==================== assignRoleToUser 测试 ====================

    @Test
    @DisplayName("分配角色给用户 - 成功分配角色")
    void assignRoleToUser_shouldAssignSuccessfully() {
        // 执行测试
        roleService.assignRoleToUser(USER_ID, ROLE_ID);

        // 验证结果
        verify(userRoleRepository).save(argThat(userRole -> {
            assertEquals(USER_ID, userRole.getUserId());
            assertEquals(ROLE_ID, userRole.getRoleId());
            return true;
        }));
    }

    // ==================== removeRoleFromUser 测试 ====================

    @Test
    @DisplayName("移除用户角色 - 成功移除角色")
    void removeRoleFromUser_shouldRemoveSuccessfully() {
        // 执行测试
        roleService.removeRoleFromUser(USER_ID, ROLE_ID);

        // 验证结果
        verify(userRoleRepository).deleteByUserIdAndRoleId(USER_ID, ROLE_ID);
    }

    // ==================== getUserRoles 测试 ====================

    @Test
    @DisplayName("获取用户角色列表 - 成功返回角色列表")
    void getUserRoles_shouldReturnUserRoles() {
        // 准备测试数据
        UserRole userRole1 = new UserRole();
        userRole1.setId(1L);
        userRole1.setUserId(USER_ID);
        userRole1.setRoleId(10L);
        UserRole userRole2 = new UserRole();
        userRole2.setId(2L);
        userRole2.setUserId(USER_ID);
        userRole2.setRoleId(20L);

        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(userRole1, userRole2));

        // 执行测试
        List<UserRole> result = roleService.getUserRoles(USER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRoleRepository).findByUserId(USER_ID);
    }
}
