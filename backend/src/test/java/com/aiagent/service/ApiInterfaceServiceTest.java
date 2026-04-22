package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.ApiInterface;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.ApiInterfaceRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApiInterfaceService 单元测试
 * 测试API接口的增删改查、启用/禁用切换等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("API接口服务测试")
class ApiInterfaceServiceTest {

    @Mock
    private ApiInterfaceRepository apiInterfaceRepository;

    @InjectMocks
    private ApiInterfaceService apiInterfaceService;

    private ApiInterface testInterface;

    @BeforeEach
    void setUp() {
        // 初始化测试API接口数据
        testInterface = new ApiInterface();
        testInterface.setId(1L);
        testInterface.setAgentId(1L);
        testInterface.setTenantId(100L);
        testInterface.setVersionId(1L);
        testInterface.setPath("/api/v1/agents");
        testInterface.setMethod("POST");
        testInterface.setDescription("Agent调用接口");
        testInterface.setIsActive(true);
        testInterface.setCreatedAt(LocalDateTime.now());
    }

    // ==================== createInterface 测试 ====================

    @Test
    @DisplayName("创建API接口 - 成功")
    void create_Success() {
        when(apiInterfaceRepository.save(any(ApiInterface.class)))
                .thenReturn(testInterface);

        ApiInterface result = apiInterfaceService.create(testInterface);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("/api/v1/agents", result.getPath());
        assertEquals("POST", result.getMethod());
        verify(apiInterfaceRepository).save(testInterface);
    }

    @Test
    @DisplayName("创建API接口 - 保存失败时抛出异常")
    void create_SaveFailure_ThrowsException() {
        when(apiInterfaceRepository.save(any(ApiInterface.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        assertThrows(RuntimeException.class,
                () -> apiInterfaceService.create(testInterface));
    }

    // ==================== updateInterface 测试 ====================

    @Test
    @DisplayName("更新API接口 - 成功")
    void update_Success() {
        ApiInterface updateDetails = new ApiInterface();
        updateDetails.setPath("/api/v2/agents");
        updateDetails.setMethod("PUT");
        updateDetails.setDescription("更新后的描述");

        when(apiInterfaceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testInterface));
        when(apiInterfaceRepository.save(any(ApiInterface.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiInterface result = apiInterfaceService.update(1L, 100L, updateDetails);

        assertNotNull(result);
        assertEquals("/api/v2/agents", result.getPath());
        assertEquals("PUT", result.getMethod());
        assertEquals("更新后的描述", result.getDescription());
        verify(apiInterfaceRepository).save(any(ApiInterface.class));
    }

    @Test
    @DisplayName("更新API接口 - 部分字段更新")
    void update_PartialUpdate() {
        ApiInterface updateDetails = new ApiInterface();
        updateDetails.setDescription("仅更新描述");

        when(apiInterfaceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testInterface));
        when(apiInterfaceRepository.save(any(ApiInterface.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiInterface result = apiInterfaceService.update(1L, 100L, updateDetails);

        assertNotNull(result);
        // 路径和方法应保持不变
        assertEquals("/api/v1/agents", result.getPath());
        assertEquals("POST", result.getMethod());
        // 仅描述被更新
        assertEquals("仅更新描述", result.getDescription());
    }

    @Test
    @DisplayName("更新API接口 - 接口不存在时抛出异常")
    void update_NotFound_ThrowsException() {
        ApiInterface updateDetails = new ApiInterface();
        updateDetails.setPath("/api/v2/agents");

        when(apiInterfaceRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> apiInterfaceService.update(999L, 100L, updateDetails));

        assertEquals(ResultCode.RESOURCE_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== deleteInterface 测试 ====================

    @Test
    @DisplayName("删除API接口 - 成功")
    void delete_Success() {
        when(apiInterfaceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testInterface));
        doNothing().when(apiInterfaceRepository).delete(any(ApiInterface.class));

        apiInterfaceService.delete(1L, 100L);

        verify(apiInterfaceRepository).delete(testInterface);
    }

    @Test
    @DisplayName("删除API接口 - 接口不存在时抛出异常")
    void delete_NotFound_ThrowsException() {
        when(apiInterfaceRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> apiInterfaceService.delete(999L, 100L));

        assertEquals(ResultCode.RESOURCE_NOT_FOUND.getCode(), exception.getCode());
        verify(apiInterfaceRepository, never()).delete(any());
    }

    // ==================== toggleInterface 测试 ====================

    @Test
    @DisplayName("切换接口状态 - 禁用成功")
    void toggleActive_Disable_Success() {
        when(apiInterfaceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testInterface));
        when(apiInterfaceRepository.save(any(ApiInterface.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiInterface result = apiInterfaceService.toggleActive(1L, 100L, false);

        assertNotNull(result);
        assertFalse(result.getIsActive());
        verify(apiInterfaceRepository).save(any(ApiInterface.class));
    }

    @Test
    @DisplayName("切换接口状态 - 启用成功")
    void toggleActive_Enable_Success() {
        testInterface.setIsActive(false);

        when(apiInterfaceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testInterface));
        when(apiInterfaceRepository.save(any(ApiInterface.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiInterface result = apiInterfaceService.toggleActive(1L, 100L, true);

        assertNotNull(result);
        assertTrue(result.getIsActive());
    }

    @Test
    @DisplayName("切换接口状态 - 接口不存在时抛出异常")
    void toggleActive_NotFound_ThrowsException() {
        when(apiInterfaceRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> apiInterfaceService.toggleActive(999L, 100L, true));

        assertEquals(ResultCode.RESOURCE_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== getInterfacesByAgent 测试 ====================

    @Test
    @DisplayName("按Agent查询接口列表 - 成功")
    void listByAgent_Success() {
        List<ApiInterface> interfaces = Arrays.asList(testInterface);
        when(apiInterfaceRepository.findByAgentIdAndTenantId(1L, 100L))
                .thenReturn(interfaces);

        List<ApiInterface> result = apiInterfaceService.listByAgent(1L, 100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/api/v1/agents", result.get(0).getPath());
    }

    @Test
    @DisplayName("按Agent查询接口列表 - 空列表")
    void listByAgent_Empty() {
        when(apiInterfaceRepository.findByAgentIdAndTenantId(1L, 100L))
                .thenReturn(Collections.emptyList());

        List<ApiInterface> result = apiInterfaceService.listByAgent(1L, 100L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("按租户分页查询接口 - 成功")
    void listByTenant_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApiInterface> page = new PageImpl<>(Arrays.asList(testInterface));

        when(apiInterfaceRepository.findByTenantId(100L, pageable))
                .thenReturn(page);

        Page<ApiInterface> result = apiInterfaceService.listByTenant(100L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("根据ID获取接口 - 成功")
    void getById_Success() {
        when(apiInterfaceRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testInterface));

        ApiInterface result = apiInterfaceService.getById(1L, 100L);

        assertNotNull(result);
        assertEquals("/api/v1/agents", result.getPath());
    }

    @Test
    @DisplayName("根据ID获取接口 - 不存在时抛出异常")
    void getById_NotFound_ThrowsException() {
        when(apiInterfaceRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> apiInterfaceService.getById(999L, 100L));

        assertEquals(ResultCode.RESOURCE_NOT_FOUND.getCode(), exception.getCode());
    }
}
