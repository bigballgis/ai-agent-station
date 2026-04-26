package com.aiagent.service;

import com.aiagent.entity.AgentMemory;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentMemoryRepository;
import com.aiagent.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MemoryService 单元测试
 * 测试记忆服务的核心方法：创建记忆、获取记忆、删除记忆、查询记忆等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent记忆服务测试")
class MemoryServiceTest {

    @Mock
    private AgentMemoryRepository memoryRepository;

    @InjectMocks
    private MemoryService memoryService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private static final Long TENANT_ID = 1L;
    private static final Long AGENT_ID = 100L;
    private static final Long MEMORY_ID = 10L;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ==================== createMemory 测试 ====================

    @Test
    @DisplayName("创建记忆 - 成功创建并设置租户ID")
    void createMemory_shouldCreateSuccessfully() {
        // 准备测试数据
        AgentMemory memory = new AgentMemory();
        memory.setAgentId(AGENT_ID);
        memory.setContent("测试记忆内容");
        memory.setMemoryType(AgentMemory.MemoryType.SHORT_TERM);

        when(memoryRepository.save(any(AgentMemory.class))).thenAnswer(inv -> {
            AgentMemory saved = inv.getArgument(0);
            saved.setId(MEMORY_ID);
            return saved;
        });

        // 执行测试
        AgentMemory result = memoryService.createMemory(memory);

        // 验证结果
        assertNotNull(result);
        assertEquals(MEMORY_ID, result.getId());
        assertEquals(TENANT_ID, result.getTenantId());
        verify(memoryRepository).save(memory);
    }

    // ==================== getMemories 测试 ====================

    @Test
    @DisplayName("获取记忆列表 - 成功返回分页记忆列表")
    void getMemories_shouldReturnPagedMemories() {
        // 准备测试数据
        AgentMemory memory1 = new AgentMemory();
        memory1.setId(1L);
        memory1.setContent("记忆1");
        AgentMemory memory2 = new AgentMemory();
        memory2.setId(2L);
        memory2.setContent("记忆2");
        Page<AgentMemory> memoryPage = new PageImpl<>(List.of(memory1, memory2));

        when(memoryRepository.searchMemories(eq(AGENT_ID), eq(TENANT_ID), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(memoryPage);

        // 执行测试
        Page<AgentMemory> result = memoryService.getMemories(AGENT_ID, null, null, 0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(memoryRepository).searchMemories(eq(AGENT_ID), eq(TENANT_ID), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("获取记忆列表 - 带关键字和类型过滤")
    void getMemories_shouldFilterByKeywordAndType() {
        // 准备测试数据
        AgentMemory memory = new AgentMemory();
        memory.setId(1L);
        Page<AgentMemory> memoryPage = new PageImpl<>(List.of(memory));

        when(memoryRepository.searchMemories(
                eq(AGENT_ID), eq(TENANT_ID), eq("关键字"),
                eq(AgentMemory.MemoryType.LONG_TERM), any(Pageable.class)))
                .thenReturn(memoryPage);

        // 执行测试
        Page<AgentMemory> result = memoryService.getMemories(AGENT_ID, "关键字",
                AgentMemory.MemoryType.LONG_TERM, 0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(memoryRepository).searchMemories(
                eq(AGENT_ID), eq(TENANT_ID), eq("关键字"),
                eq(AgentMemory.MemoryType.LONG_TERM), any(Pageable.class));
    }

    // ==================== getMemory 测试 ====================

    @Test
    @DisplayName("根据ID获取记忆 - 成功返回记忆")
    void getMemory_shouldReturnMemory() {
        // 准备测试数据
        AgentMemory memory = new AgentMemory();
        memory.setId(MEMORY_ID);
        memory.setContent("测试记忆");

        when(memoryRepository.findById(MEMORY_ID)).thenReturn(Optional.of(memory));

        // 执行测试
        AgentMemory result = memoryService.getMemory(MEMORY_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(MEMORY_ID, result.getId());
        assertEquals("测试记忆", result.getContent());
    }

    @Test
    @DisplayName("根据ID获取记忆 - 记忆不存在时抛出异常")
    void getMemory_shouldThrowExceptionWhenNotFound() {
        // 模拟记忆不存在
        when(memoryRepository.findById(MEMORY_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> memoryService.getMemory(MEMORY_ID));

        assertEquals("记忆不存在", exception.getMessage());
    }

    // ==================== deleteMemory 测试 ====================

    @Test
    @DisplayName("删除记忆 - 成功删除")
    void deleteMemory_shouldDeleteSuccessfully() {
        // 执行测试
        memoryService.deleteMemory(MEMORY_ID);

        // 验证结果
        verify(memoryRepository).deleteById(MEMORY_ID);
    }

    // ==================== cleanupExpiredMemories 测试 ====================

    @Test
    @DisplayName("清理过期记忆 - 成功清理")
    void cleanupExpiredMemories_shouldCleanupSuccessfully() {
        // 执行测试
        memoryService.cleanupExpiredMemories();

        // 验证结果
        verify(memoryRepository).deleteByExpiresAtBeforeAndTenantId(any(LocalDateTime.class), eq(TENANT_ID));
    }

    // ==================== scheduledCleanup 测试 ====================

    @Test
    @DisplayName("定时清理任务 - 成功执行清理")
    void scheduledCleanup_shouldExecuteCleanup() {
        // 执行测试
        memoryService.scheduledCleanup();

        // 验证结果
        verify(memoryRepository).deleteByExpiresAtBeforeAndTenantId(any(LocalDateTime.class), eq(TENANT_ID));
    }

    // ==================== countMemories 测试 ====================

    @Test
    @DisplayName("统计Agent的记忆数量 - 成功返回数量")
    void countMemories_shouldReturnCount() {
        when(memoryRepository.countByAgentIdAndTenantId(AGENT_ID, TENANT_ID)).thenReturn(5L);

        // 执行测试
        long count = memoryService.countMemories(AGENT_ID);

        // 验证结果
        assertEquals(5L, count);
        verify(memoryRepository).countByAgentIdAndTenantId(AGENT_ID, TENANT_ID);
    }
}
