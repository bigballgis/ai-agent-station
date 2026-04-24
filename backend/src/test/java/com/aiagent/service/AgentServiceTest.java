package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentService 单元测试
 * 测试 Agent 的增删改查、复制、版本管理、分页查询等功能
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

    private static final Long AGENT_ID = 1L;
    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // 初始化测试Agent
        testAgent = new Agent();
        testAgent.setId(AGENT_ID);
        testAgent.setTenantId(TENANT_ID);
        testAgent.setName("测试Agent");
        testAgent.setDescription("测试描述");
        testAgent.setConfig(new HashMap<>(Map.of("model", "gpt-4", "temperature", 0.7)));
        testAgent.setIsActive(true);
        testAgent.setStatus(Agent.AgentStatus.DRAFT);
        testAgent.setCreatedBy(USER_ID);
        testAgent.setUpdatedBy(USER_ID);

        // 模拟租户上下文
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(TENANT_ID);

        // 模拟已认证用户
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, "admin", TENANT_ID);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
        SecurityContextHolder.clearContext();
    }

    // ==================== 创建Agent测试 ====================

    @Test
    @DisplayName("创建Agent - 有效数据，成功创建")
    void testCreateAgent_ValidData_Success() {
        when(agentRepository.existsByNameAndTenantId("新Agent", TENANT_ID)).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> {
            Agent agent = inv.getArgument(0);
            agent.setId(2L);
            return agent;
        });
        when(agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent newAgent = new Agent();
        newAgent.setName("新Agent");
        newAgent.setDescription("新Agent描述");
        newAgent.setConfig(Map.of("key", "value"));

        Agent result = agentService.createAgent(newAgent);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("新Agent", result.getName());
        assertEquals(TENANT_ID, result.getTenantId());
        assertTrue(result.getIsActive());
        assertEquals(USER_ID, result.getCreatedBy());
        verify(agentRepository).save(any(Agent.class));
        verify(agentVersionRepository).save(any(AgentVersion.class));
    }

    @Test
    @DisplayName("创建Agent - 名称已存在，抛出BusinessException")
    void testCreateAgent_DuplicateName_ThrowsException() {
        when(agentRepository.existsByNameAndTenantId("测试Agent", TENANT_ID)).thenReturn(true);

        Agent newAgent = new Agent();
        newAgent.setName("测试Agent");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                agentService.createAgent(newAgent)
        );
        assertEquals(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("Agent名称已存在"));
        verify(agentRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建Agent - config为null时自动初始化为空Map")
    void testCreateAgent_NullConfig_InitializedToEmptyMap() {
        when(agentRepository.existsByNameAndTenantId("ConfigNull", TENANT_ID)).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> {
            Agent agent = inv.getArgument(0);
            agent.setId(3L);
            return agent;
        });
        when(agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent newAgent = new Agent();
        newAgent.setName("ConfigNull");
        newAgent.setConfig(null);

        Agent result = agentService.createAgent(newAgent);

        assertNotNull(result.getConfig());
        assertTrue(result.getConfig().isEmpty());
    }

    // ==================== 更新Agent测试 ====================

    @Test
    @DisplayName("更新Agent - 成功更新名称、描述和配置")
    void testUpdateAgent_Success() {
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(AGENT_ID))
                .thenReturn(Optional.empty());
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent updateData = new Agent();
        updateData.setName("更新后的Agent");
        updateData.setDescription("更新后的描述");
        updateData.setConfig(Map.of("model", "gpt-3.5-turbo"));
        updateData.setIsActive(false);

        Agent result = agentService.updateAgent(AGENT_ID, updateData);

        assertNotNull(result);
        assertEquals("更新后的Agent", result.getName());
        assertEquals("更新后的描述", result.getDescription());
        assertEquals(Map.of("model", "gpt-3.5-turbo"), result.getConfig());
        assertFalse(result.getIsActive());
        verify(agentVersionRepository).save(any(AgentVersion.class));
    }

    @Test
    @DisplayName("更新Agent - 不存在，抛出ResourceNotFoundException")
    void testUpdateAgent_NotFound_ThrowsException() {
        when(agentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        Agent updateData = new Agent();
        updateData.setName("不存在的Agent");

        assertThrows(ResourceNotFoundException.class, () ->
                agentService.updateAgent(999L, updateData)
        );
    }

    // ==================== 删除Agent测试 ====================

    @Test
    @DisplayName("删除Agent - 成功删除")
    void testDeleteAgent_Success() {
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID)).thenReturn(Optional.of(testAgent));
        doNothing().when(agentRepository).delete(testAgent);

        agentService.deleteAgent(AGENT_ID);

        verify(agentRepository).delete(testAgent);
    }

    @Test
    @DisplayName("删除Agent - 不存在，抛出ResourceNotFoundException")
    void testDeleteAgent_NotFound_ThrowsException() {
        when(agentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                agentService.deleteAgent(999L)
        );
        verify(agentRepository, never()).delete(any());
    }

    // ==================== 复制Agent测试 ====================

    @Test
    @DisplayName("复制Agent - 成功复制，新Agent名称和描述正确")
    void testCopyAgent_Success() {
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID)).thenReturn(Optional.of(testAgent));
        when(agentRepository.existsByNameAndTenantId("复制Agent", TENANT_ID)).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> {
            Agent agent = inv.getArgument(0);
            agent.setId(3L);
            return agent;
        });
        when(agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent result = agentService.copyAgent(AGENT_ID, "复制Agent");

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("复制Agent", result.getName());
        assertTrue(result.getDescription().contains("复制自: 测试Agent"));
        assertEquals(testAgent.getConfig(), result.getConfig());
        assertTrue(result.getIsActive());
        verify(agentRepository).save(any(Agent.class));
        verify(agentVersionRepository).save(any(AgentVersion.class));
    }

    @Test
    @DisplayName("复制Agent - 新名称已存在，抛出BusinessException")
    void testCopyAgent_DuplicateName_ThrowsException() {
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID)).thenReturn(Optional.of(testAgent));
        when(agentRepository.existsByNameAndTenantId("已存在名称", TENANT_ID)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                agentService.copyAgent(AGENT_ID, "已存在名称")
        );
        assertEquals(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(agentRepository, never()).save(any());
    }

    @Test
    @DisplayName("复制Agent - 原Agent不存在，抛出ResourceNotFoundException")
    void testCopyAgent_OriginalNotFound_ThrowsException() {
        when(agentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                agentService.copyAgent(999L, "新名称")
        );
    }

    // ==================== 查询Agent测试 ====================

    @Test
    @DisplayName("根据ID获取Agent - 存在时返回")
    void testGetAgentById_Found() {
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID)).thenReturn(Optional.of(testAgent));

        Agent result = agentService.getAgentById(AGENT_ID);

        assertNotNull(result);
        assertEquals("测试Agent", result.getName());
        assertEquals(TENANT_ID, result.getTenantId());
    }

    @Test
    @DisplayName("根据ID获取Agent - 不存在时抛出异常")
    void testGetAgentById_NotFound() {
        when(agentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                agentService.getAgentById(999L)
        );
    }

    @Test
    @DisplayName("获取所有Agent列表 - 返回租户下所有Agent")
    void testGetAllAgents_Success() {
        Agent agent2 = new Agent();
        agent2.setId(2L);
        agent2.setTenantId(TENANT_ID);
        agent2.setName("Agent2");

        when(agentRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(testAgent, agent2));

        List<Agent> result = agentService.getAllAgents();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(agentRepository).findByTenantId(TENANT_ID);
    }

    // ==================== 分页查询测试 ====================

    @Test
    @DisplayName("分页查询Agent - 带关键词和状态过滤")
    void testGetAgentsPaged_WithFilters() {
        Agent agent2 = new Agent();
        agent2.setId(2L);
        agent2.setTenantId(TENANT_ID);
        agent2.setName("搜索结果Agent");
        agent2.setStatus(Agent.AgentStatus.PUBLISHED);

        Page<Agent> mockPage = new PageImpl<>(List.of(agent2));
        when(agentRepository.findByTenantIdWithFilters(eq(TENANT_ID), eq("搜索"), eq("PUBLISHED"), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Agent> result = agentService.getAgentsPaged(TENANT_ID, "搜索", "PUBLISHED", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("搜索结果Agent", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("分页查询Agent - 无过滤条件返回全部")
    void testGetAgentsPaged_NoFilters() {
        Page<Agent> mockPage = new PageImpl<>(List.of(testAgent));
        when(agentRepository.findByTenantIdWithFilters(eq(TENANT_ID), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Agent> result = agentService.getAgentsPaged(TENANT_ID, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== 版本管理测试 ====================

    @Test
    @DisplayName("获取Agent版本列表 - 按版本号降序返回")
    void testGetAgentVersions_Success() {
        AgentVersion v2 = new AgentVersion();
        v2.setVersionNumber(2);
        AgentVersion v1 = new AgentVersion();
        v1.setVersionNumber(1);

        when(agentVersionRepository.findByAgentIdOrderByVersionNumberDesc(AGENT_ID))
                .thenReturn(List.of(v2, v1));

        List<AgentVersion> result = agentService.getAgentVersions(AGENT_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getVersionNumber());
        assertEquals(1, result.get(1).getVersionNumber());
    }

    @Test
    @DisplayName("获取指定版本 - 存在时返回")
    void testGetAgentVersion_Found() {
        AgentVersion version = new AgentVersion();
        version.setVersionNumber(1);
        version.setConfig(Map.of("model", "gpt-4"));

        when(agentVersionRepository.findByAgentIdAndVersionNumber(AGENT_ID, 1))
                .thenReturn(Optional.of(version));

        AgentVersion result = agentService.getAgentVersion(AGENT_ID, 1);

        assertNotNull(result);
        assertEquals(1, result.getVersionNumber());
    }

    @Test
    @DisplayName("获取指定版本 - 不存在时抛出异常")
    void testGetAgentVersion_NotFound() {
        when(agentVersionRepository.findByAgentIdAndVersionNumber(AGENT_ID, 99))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                agentService.getAgentVersion(AGENT_ID, 99)
        );
    }

    @Test
    @DisplayName("回滚到指定版本 - 成功回滚配置")
    void testRollbackToVersion_Success() {
        AgentVersion version = new AgentVersion();
        version.setVersionNumber(1);
        version.setConfig(Map.of("model", "gpt-3.5-turbo", "temperature", 0.5));

        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID)).thenReturn(Optional.of(testAgent));
        when(agentVersionRepository.findByAgentIdAndVersionNumber(AGENT_ID, 1))
                .thenReturn(Optional.of(version));
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentVersionRepository.findFirstByAgentIdOrderByVersionNumberDesc(AGENT_ID))
                .thenReturn(Optional.of(version));
        when(agentVersionRepository.save(any(AgentVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent result = agentService.rollbackToVersion(AGENT_ID, 1);

        assertNotNull(result);
        assertEquals(Map.of("model", "gpt-3.5-turbo", "temperature", 0.5), result.getConfig());
        verify(agentVersionRepository).save(argThat(v ->
                v.getChangeLog().contains("回滚到版本 1")
        ));
    }

    // ==================== findAgentById 测试 ====================

    @Test
    @DisplayName("findAgentById - 存在时返回Optional")
    void testFindAgentById_Found() {
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID)).thenReturn(Optional.of(testAgent));

        Optional<Agent> result = agentService.findAgentById(AGENT_ID);

        assertTrue(result.isPresent());
        assertEquals("测试Agent", result.get().getName());
    }

    @Test
    @DisplayName("findAgentById - 不存在时返回空Optional")
    void testFindAgentById_NotFound() {
        when(agentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        Optional<Agent> result = agentService.findAgentById(999L);

        assertFalse(result.isPresent());
    }
}
