package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentApproval;
import com.aiagent.entity.AgentVersion;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentApprovalRepository;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentVersionRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentApprovalService 单元测试
 * 测试审批服务的核心方法：提交审批、审批通过、审批拒绝、查询审批等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agent审批服务测试")
class AgentApprovalServiceTest {

    @Mock
    private AgentApprovalRepository agentApprovalRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentVersionRepository agentVersionRepository;

    @Mock
    private AgentTestResultService testResultService;

    @InjectMocks
    private AgentApprovalService agentApprovalService;

    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    private static final Long TENANT_ID = 1L;
    private static final Long AGENT_ID = 100L;
    private static final Long VERSION_ID = 200L;
    private static final Long SUBMITTER_ID = 300L;
    private static final Long APPROVER_ID = 400L;
    private static final Long APPROVAL_ID = 500L;

    @BeforeEach
    void setUp() {
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    // ==================== getApprovals 测试 ====================

    @Test
    @DisplayName("获取审批列表 - 成功返回分页数据")
    void getApprovals_shouldReturnPagedApprovals() {
        // 准备测试数据
        AgentApproval approval = new AgentApproval();
        approval.setId(APPROVAL_ID);
        approval.setAgentId(AGENT_ID);
        approval.setTenantId(TENANT_ID);
        Page<AgentApproval> expectedPage = new PageImpl<>(List.of(approval));
        Pageable pageable = Pageable.unpaged();

        when(agentApprovalRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(expectedPage);

        // 执行测试
        Page<AgentApproval> result = agentApprovalService.getApprovals(pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(agentApprovalRepository).findByTenantId(TENANT_ID, pageable);
    }

    // ==================== getPendingApprovals 测试 ====================

    @Test
    @DisplayName("获取待审批列表 - 成功返回待审批数据")
    void getPendingApprovals_shouldReturnPendingApprovals() {
        // 准备测试数据
        AgentApproval pendingApproval = new AgentApproval();
        pendingApproval.setId(APPROVAL_ID);
        pendingApproval.setStatus(AgentApproval.ApprovalStatus.PENDING);
        Page<AgentApproval> expectedPage = new PageImpl<>(List.of(pendingApproval));
        Pageable pageable = Pageable.unpaged();

        when(agentApprovalRepository.findByTenantIdAndStatus(TENANT_ID, AgentApproval.ApprovalStatus.PENDING, pageable))
                .thenReturn(expectedPage);

        // 执行测试
        Page<AgentApproval> result = agentApprovalService.getPendingApprovals(pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(AgentApproval.ApprovalStatus.PENDING, result.getContent().get(0).getStatus());
        verify(agentApprovalRepository).findByTenantIdAndStatus(TENANT_ID, AgentApproval.ApprovalStatus.PENDING, pageable);
    }

    // ==================== getApprovalById 测试 ====================

    @Test
    @DisplayName("根据ID获取审批记录 - 成功返回审批记录")
    void getApprovalById_shouldReturnApproval() {
        // 准备测试数据
        AgentApproval approval = new AgentApproval();
        approval.setId(APPROVAL_ID);
        approval.setAgentId(AGENT_ID);

        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.of(approval));

        // 执行测试
        AgentApproval result = agentApprovalService.getApprovalById(APPROVAL_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(APPROVAL_ID, result.getId());
        verify(agentApprovalRepository).findByIdAndTenantId(APPROVAL_ID, TENANT_ID);
    }

    @Test
    @DisplayName("根据ID获取审批记录 - 审批记录不存在时抛出异常")
    void getApprovalById_shouldThrowExceptionWhenNotFound() {
        // 模拟审批记录不存在
        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.getApprovalById(APPROVAL_ID));

        assertEquals("审批记录不存在", exception.getMessage());
        verify(agentApprovalRepository).findByIdAndTenantId(APPROVAL_ID, TENANT_ID);
    }

    // ==================== getApprovalsByAgentId 测试 ====================

    @Test
    @DisplayName("根据AgentId获取审批列表 - 成功返回审批列表")
    void getApprovalsByAgentId_shouldReturnApprovals() {
        // 准备测试数据
        AgentApproval approval1 = new AgentApproval();
        approval1.setId(1L);
        AgentApproval approval2 = new AgentApproval();
        approval2.setId(2L);

        when(agentApprovalRepository.findByAgentIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Arrays.asList(approval1, approval2));

        // 执行测试
        List<AgentApproval> result = agentApprovalService.getApprovalsByAgentId(AGENT_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(agentApprovalRepository).findByAgentIdAndTenantId(AGENT_ID, TENANT_ID);
    }

    // ==================== submitForApproval 测试 ====================

    @Test
    @DisplayName("提交审批 - Agent为草稿状态且测试通过率>=80%时成功提交")
    void submitForApproval_shouldSucceedWhenAgentIsDraftAndPassRateIsHigh() {
        // 准备测试数据
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setStatus(Agent.AgentStatus.DRAFT);

        AgentVersion version = new AgentVersion();
        version.setId(VERSION_ID);

        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.of(agent));
        when(testResultService.getPassRateByAgent(AGENT_ID)).thenReturn(90.0);
        when(agentVersionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(agentApprovalRepository.save(any(AgentApproval.class))).thenAnswer(inv -> {
            AgentApproval saved = inv.getArgument(0);
            saved.setId(APPROVAL_ID);
            return saved;
        });

        // 执行测试
        AgentApproval result = agentApprovalService.submitForApproval(AGENT_ID, VERSION_ID, "请审批", SUBMITTER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(AgentApproval.ApprovalStatus.PENDING, result.getStatus());
        assertEquals(AGENT_ID, result.getAgentId());
        assertEquals(VERSION_ID, result.getAgentVersionId());
        assertEquals(SUBMITTER_ID, result.getSubmitterId());
        assertEquals("请审批", result.getRemark());
        verify(agentApprovalRepository).save(any(AgentApproval.class));
        verify(agentRepository).save(agent);
        assertEquals(Agent.AgentStatus.PENDING_APPROVAL, agent.getStatus());
    }

    @Test
    @DisplayName("提交审批 - Agent为已审批状态时成功提交")
    void submitForApproval_shouldSucceedWhenAgentIsApproved() {
        // 准备测试数据
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setStatus(Agent.AgentStatus.APPROVED);

        AgentVersion version = new AgentVersion();
        version.setId(VERSION_ID);

        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.of(agent));
        when(testResultService.getPassRateByAgent(AGENT_ID)).thenReturn(85.0);
        when(agentVersionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(agentApprovalRepository.save(any(AgentApproval.class))).thenAnswer(inv -> {
            AgentApproval saved = inv.getArgument(0);
            saved.setId(APPROVAL_ID);
            return saved;
        });

        // 执行测试
        AgentApproval result = agentApprovalService.submitForApproval(AGENT_ID, VERSION_ID, "重新提交", SUBMITTER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(AgentApproval.ApprovalStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("提交审批 - Agent不存在时抛出异常")
    void submitForApproval_shouldThrowExceptionWhenAgentNotFound() {
        // 模拟Agent不存在
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.submitForApproval(AGENT_ID, VERSION_ID, "请审批", SUBMITTER_ID));

        assertEquals("Agent不存在", exception.getMessage());
        verify(agentApprovalRepository, never()).save(any());
    }

    @Test
    @DisplayName("提交审批 - Agent状态不允许提交时抛出异常")
    void submitForApproval_shouldThrowExceptionWhenStatusNotAllowed() {
        // 准备测试数据 - Agent为已发布状态，不允许提交审批
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setStatus(Agent.AgentStatus.PUBLISHED);

        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.of(agent));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.submitForApproval(AGENT_ID, VERSION_ID, "请审批", SUBMITTER_ID));

        assertEquals("当前状态不允许提交审批", exception.getMessage());
        verify(agentApprovalRepository, never()).save(any());
    }

    @Test
    @DisplayName("提交审批 - 测试通过率低于80%时抛出异常")
    void submitForApproval_shouldThrowExceptionWhenPassRateTooLow() {
        // 准备测试数据
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setStatus(Agent.AgentStatus.DRAFT);

        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.of(agent));
        when(testResultService.getPassRateByAgent(AGENT_ID)).thenReturn(60.0);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.submitForApproval(AGENT_ID, VERSION_ID, "请审批", SUBMITTER_ID));

        assertEquals("测试通过率低于80%，不允许提交审批", exception.getMessage());
        verify(agentApprovalRepository, never()).save(any());
    }

    @Test
    @DisplayName("提交审批 - 版本不存在时抛出异常")
    void submitForApproval_shouldThrowExceptionWhenVersionNotFound() {
        // 准备测试数据
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setStatus(Agent.AgentStatus.DRAFT);

        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.of(agent));
        when(testResultService.getPassRateByAgent(AGENT_ID)).thenReturn(90.0);
        when(agentVersionRepository.findById(VERSION_ID)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.submitForApproval(AGENT_ID, VERSION_ID, "请审批", SUBMITTER_ID));

        assertEquals("版本不存在", exception.getMessage());
        verify(agentApprovalRepository, never()).save(any());
    }

    // ==================== approve 测试 ====================

    @Test
    @DisplayName("审批通过 - 成功审批通过并更新Agent状态")
    void approve_shouldApproveSuccessfully() {
        // 准备测试数据
        AgentApproval approval = new AgentApproval();
        approval.setId(APPROVAL_ID);
        approval.setAgentId(AGENT_ID);
        approval.setStatus(AgentApproval.ApprovalStatus.PENDING);

        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setStatus(Agent.AgentStatus.PENDING_APPROVAL);

        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.of(approval));
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.of(agent));
        when(agentApprovalRepository.save(any(AgentApproval.class))).thenReturn(approval);

        // 执行测试
        AgentApproval result = agentApprovalService.approve(APPROVAL_ID, "审批通过", APPROVER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(AgentApproval.ApprovalStatus.APPROVED, result.getStatus());
        assertEquals(APPROVER_ID, result.getApproverId());
        assertEquals("审批通过", result.getApprovalRemark());
        assertNotNull(result.getApprovedAt());
        verify(agentRepository).save(agent);
        assertEquals(Agent.AgentStatus.APPROVED, agent.getStatus());
    }

    @Test
    @DisplayName("审批通过 - 审批记录不存在时抛出异常")
    void approve_shouldThrowExceptionWhenApprovalNotFound() {
        // 模拟审批记录不存在
        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.approve(APPROVAL_ID, "审批通过", APPROVER_ID));

        assertEquals("审批记录不存在", exception.getMessage());
    }

    @Test
    @DisplayName("审批通过 - 审批状态非待审批时抛出异常")
    void approve_shouldThrowExceptionWhenStatusNotPending() {
        // 准备测试数据 - 审批已通过
        AgentApproval approval = new AgentApproval();
        approval.setId(APPROVAL_ID);
        approval.setStatus(AgentApproval.ApprovalStatus.APPROVED);

        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.of(approval));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.approve(APPROVAL_ID, "重复审批", APPROVER_ID));

        assertEquals("当前状态不允许审批", exception.getMessage());
        verify(agentApprovalRepository, never()).save(any());
    }

    @Test
    @DisplayName("审批通过 - Agent不存在时抛出异常")
    void approve_shouldThrowExceptionWhenAgentNotFound() {
        // 准备测试数据
        AgentApproval approval = new AgentApproval();
        approval.setId(APPROVAL_ID);
        approval.setAgentId(AGENT_ID);
        approval.setStatus(AgentApproval.ApprovalStatus.PENDING);

        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.of(approval));
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.approve(APPROVAL_ID, "审批通过", APPROVER_ID));

        assertEquals("Agent不存在", exception.getMessage());
    }

    // ==================== reject 测试 ====================

    @Test
    @DisplayName("审批拒绝 - 成功拒绝审批并更新Agent状态为草稿")
    void reject_shouldRejectSuccessfully() {
        // 准备测试数据
        AgentApproval approval = new AgentApproval();
        approval.setId(APPROVAL_ID);
        approval.setAgentId(AGENT_ID);
        approval.setStatus(AgentApproval.ApprovalStatus.PENDING);

        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setStatus(Agent.AgentStatus.PENDING_APPROVAL);

        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.of(approval));
        when(agentRepository.findByIdAndTenantId(AGENT_ID, TENANT_ID))
                .thenReturn(Optional.of(agent));
        when(agentApprovalRepository.save(any(AgentApproval.class))).thenReturn(approval);

        // 执行测试
        AgentApproval result = agentApprovalService.reject(APPROVAL_ID, "不符合要求", APPROVER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(AgentApproval.ApprovalStatus.REJECTED, result.getStatus());
        assertEquals(APPROVER_ID, result.getApproverId());
        assertEquals("不符合要求", result.getApprovalRemark());
        assertNotNull(result.getApprovedAt());
        verify(agentRepository).save(agent);
        assertEquals(Agent.AgentStatus.DRAFT, agent.getStatus());
    }

    @Test
    @DisplayName("审批拒绝 - 审批记录不存在时抛出异常")
    void reject_shouldThrowExceptionWhenApprovalNotFound() {
        // 模拟审批记录不存在
        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.reject(APPROVAL_ID, "拒绝", APPROVER_ID));

        assertEquals("审批记录不存在", exception.getMessage());
    }

    @Test
    @DisplayName("审批拒绝 - 审批状态非待审批时抛出异常")
    void reject_shouldThrowExceptionWhenStatusNotPending() {
        // 准备测试数据 - 审批已拒绝
        AgentApproval approval = new AgentApproval();
        approval.setId(APPROVAL_ID);
        approval.setStatus(AgentApproval.ApprovalStatus.REJECTED);

        when(agentApprovalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                .thenReturn(Optional.of(approval));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> agentApprovalService.reject(APPROVAL_ID, "重复拒绝", APPROVER_ID));

        assertEquals("当前状态不允许审批", exception.getMessage());
        verify(agentApprovalRepository, never()).save(any());
    }
}
