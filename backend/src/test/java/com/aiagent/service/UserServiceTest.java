package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.User;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 * 测试用户创建、更新、删除、查询、密码重置等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encoded_password");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setTenantId(100L);
        testUser.setIsActive(true);
    }

    @Test
    @DisplayName("创建用户 - 成功")
    void testCreateUser_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("raw_password")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("raw_password");
        newUser.setEmail("test@example.com");

        User created = userService.createUser(newUser);

        assertNotNull(created);
        assertEquals("encoded_password", created.getPassword());
        assertTrue(created.getIsActive());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("创建用户 - 用户名已存在抛出异常")
    void testCreateUser_DuplicateUsername() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("raw_password");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(newUser);
        });

        assertTrue(exception.getMessage().contains("用户名已存在"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("更新用户 - 成功")
    void testUpdateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updateData = new User();
        updateData.setEmail("newemail@example.com");
        updateData.setPhone("13900139000");

        User updated = userService.updateUser(1L, updateData);

        assertNotNull(updated);
        assertEquals("newemail@example.com", updated.getEmail());
        assertEquals("13900139000", updated.getPhone());
    }

    @Test
    @DisplayName("更新用户 - 用户不存在抛出异常")
    void testUpdateUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        User updateData = new User();
        updateData.setEmail("new@example.com");

        assertThrows(BusinessException.class, () -> {
            userService.updateUser(999L, updateData);
        });
    }

    @Test
    @DisplayName("删除用户 - 逻辑删除(设置isActive为false)")
    void testDeleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteUser(1L);

        assertFalse(testUser.getIsActive());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("删除用户 - 用户不存在抛出异常")
    void testDeleteUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            userService.deleteUser(999L);
        });
    }

    @Test
    @DisplayName("根据用户名查询 - 成功")
    void testFindByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User found = userService.getUserByUsername("testuser");

        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
    }

    @Test
    @DisplayName("根据用户名查询 - 用户不存在抛出异常")
    void testFindByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            userService.getUserByUsername("nonexistent");
        });
    }

    @Test
    @DisplayName("重置密码 - 成功")
    void testResetPassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("new_password_123")).thenReturn("new_encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.resetPassword(1L, "new_password_123");

        assertEquals("new_encoded_password", testUser.getPassword());
        verify(passwordEncoder).encode("new_password_123");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("重置密码 - 用户不存在抛出异常")
    void testResetPassword_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            userService.resetPassword(999L, "new_password");
        });
    }

    @Test
    @DisplayName("获取所有用户 - 成功")
    void testGetAllUsers_Success() {
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        when(userRepository.findAll()).thenReturn(List.of(testUser, user2));

        List<User> users = userService.getAllUsers();

        assertNotNull(users);
        assertEquals(2, users.size());
    }
}
