package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentVersionRepository;
import com.aiagent.security.UserPrincipal;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentService 单元测试
 * 测试 Agent 的增删改查、复制、版本管理等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent服务测试")
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentVersionRepository agentVersionRepository;

    @InjectMocks
    private AgentService agentService;

    private Agent testAgent;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        // 初始化测试Agent数据
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setTenantId(100L);
        testAgent.setName("测试Agent");
        testAgent.setDescription("测试描述");
        testAgent.setConfig(new HashMap<>());
        testAgent.setIsActive(true);

        // 模拟租户上下文
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(100L);

        // 模拟已认证用户
        UserPrincipal userPrincipal = new UserPrincipal(1L, "admin", "admin@test.com", null);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("创建Agent - 成功")
    void testCreateAgent() {
        when(agentRepository.existsByNameAndTenantId("新Agent", 100L)).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(2L);
            return agent;
        });
        when(agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent newAgent = new Agent();
        newAgent.setName("新Agent");
        newAgent.setDescription("新Agent描述");

        Agent result = agentService.createAgent(newAgent);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("新Agent", result.getName());
        assertTrue(result.getIsActive());
        assertEquals(100L, result.getTenantId());
        verify(agentRepository).save(any(Agent.class));
        verify(agentVersionRepository).save(any(AgentVersion.class));
    }

    @Test
    @DisplayName("创建Agent - 名称已存在抛出异常")
    void testCreateAgent_DuplicateName() {
        when(agentRepository.existsByNameAndTenantId("测试Agent", 100L)).thenReturn(true);

        Agent newAgent = new Agent();
        newAgent.setName("测试Agent");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            agentService.createAgent(newAgent);
        });

        assertEquals(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("Agent名称已存在"));
        verify(agentRepository, never()).save(any());
    }

    @Test
    @DisplayName("根据ID获取Agent - 成功")
    void testGetAgentById() {
        when(agentRepository.findByIdAndTenantId(1L, 100L)).thenReturn(Optional.of(testAgent));

        Agent result = agentService.getAgentById(1L);

        assertNotNull(result);
        assertEquals("测试Agent", result.getName());
        assertEquals(100L, result.getTenantId());
    }

    @Test
    @DisplayName("根据ID获取Agent - 不存在抛出异常")
    void testGetAgentById_NotFound() {
        when(agentRepository.findByIdAndTenantId(999L, 100L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> agentService.getAgentById(999L));
    }

    @Test
    @DisplayName("更新Agent - 成功")
    void testUpdateAgent() {
        when(agentRepository.findByIdAndTenantId(1L, 100L)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(1L))
                .thenReturn(Optional.empty());
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent updateData = new Agent();
        updateData.setName("更新后的Agent");
        updateData.setDescription("更新后的描述");
        updateData.setConfig(Map.of("key", "value"));
        updateData.setIsActive(false);

        Agent result = agentService.updateAgent(1L, updateData);

        assertNotNull(result);
        assertEquals("更新后的Agent", result.getName());
        assertEquals("更新后的描述", result.getDescription());
        assertFalse(result.getIsActive());
    }

    @Test
    @DisplayName("删除Agent - 成功")
    void testDeleteAgent() {
        when(agentRepository.findByIdAndTenantId(1L, 100L)).thenReturn(Optional.of(testAgent));
        doNothing().when(agentRepository).delete(testAgent);

        agentService.deleteAgent(1L);

        verify(agentRepository).delete(testAgent);
    }

    @Test
    @DisplayName("获取所有Agent列表 - 成功")
    void testGetAllAgents() {
        Agent agent2 = new Agent();
        agent2.setId(2L);
        agent2.setTenantId(100L);
        agent2.setName("Agent2");

        when(agentRepository.findByTenantId(100L)).thenReturn(List.of(testAgent, agent2));

        List<Agent> result = agentService.getAllAgents();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("复制Agent - 成功")
    void testCopyAgent() {
        when(agentRepository.findByIdAndTenantId(1L, 100L)).thenReturn(Optional.of(testAgent));
        when(agentRepository.existsByNameAndTenantId("复制Agent", 100L)).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> {
            Agent agent = inv.getArgument(0);
            agent.setId(3L);
            return agent;
        });
        when(agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent result = agentService.copyAgent(1L, "复制Agent");

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("复制Agent", result.getName());
        assertTrue(result.getDescription().contains("复制自"));
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    @DisplayName("复制Agent - 新名称已存在抛出异常")
    void testCopyAgent_DuplicateName() {
        when(agentRepository.findByIdAndTenantId(1L, 100L)).thenReturn(Optional.of(testAgent));
        when(agentRepository.existsByNameAndTenantId("已存在名称", 100L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> agentService.copyAgent(1L, "已存在名称"));
    }
}
