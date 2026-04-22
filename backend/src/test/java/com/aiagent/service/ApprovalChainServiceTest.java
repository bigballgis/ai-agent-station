package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentApproval;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentApprovalRepository;
import com.aiagent.repository.AgentRepository;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ApprovalChainService 单元测试
 * 测试审批链的启动、审批、拒绝、获取当前步骤等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("审批链服务测试")
class ApprovalChainServiceTest {

    @Mock
    private AgentApprovalRepository agentApprovalRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentTestResultService testResultService;

    @InjectMocks
    private AgentApprovalService approvalService;

    private Agent testAgent;
    private AgentApproval testApproval;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setTenantId(100L);
        testAgent.setName("测试Agent");
        testAgent.setStatus(Agent.AgentStatus.DRAFT);

        testApproval = new AgentApproval();
        testApproval.setId(1L);
        testApproval.setAgentId(1L);
        testApproval.setTenantId(100L);
        testApproval.setStatus(AgentApproval.ApprovalStatus.PENDING);
        testApproval.setSubmitterId(1L);

        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(100L);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    @Test
    @DisplayName("启动审批 - 成功")
    void testStartApproval() {
        when(agentRepository.findByIdAndTenantId(1L, 100L)).thenReturn(Optional.of(testAgent));
        when(testResultService.getPassRateByAgent(1L)).thenReturn(90.0);
        when(agentApprovalRepository.save(any(AgentApproval.class))).thenAnswer(inv -> {
            AgentApproval approval = inv.getArgument(0);
            approval.setId(1L);
            return approval;
        });
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

        AgentApproval result = approvalService.submitForApproval(1L, 1L, "请审批", 1L);

        assertNotNull(result);
        assertEquals(AgentApproval.ApprovalStatus.PENDING, result.getStatus());
        assertEquals(1L, result.getSubmitterId());
        verify(agentApprovalRepository).save(any(AgentApproval.class));
    }

    @Test
    @DisplayName("启动审批 - Agent不存在抛出异常")
    void testStartApproval_AgentNotFound() {
        when(agentRepository.findByIdAndTenantId(999L, 100L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                approvalService.submitForApproval(999L, 1L, "请审批", 1L)
        );
    }

    @Test
    @DisplayName("启动审批 - 测试通过率不足80%抛出异常")
    void testStartApproval_LowPassRate() {
        when(agentRepository.findByIdAndTenantId(1L, 100L)).thenReturn(Optional.of(testAgent));
        when(testResultService.getPassRateByAgent(1L)).thenReturn(60.0);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                approvalService.submitForApproval(1L, 1L, "请审批", 1L)
        );

        assertTrue(exception.getMessage().contains("测试通过率低于80%"));
    }

    @Test
    @DisplayName("审批通过 - 成功")
    void testApproveStep() {
        when(agentApprovalRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testApproval));
        when(agentRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testAgent));
        when(agentApprovalRepository.save(any(AgentApproval.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(agentRepository.save(any(Agent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgentApproval result = approvalService.approve(1L, "审批通过", 2L);

        assertNotNull(result);
        assertEquals(AgentApproval.ApprovalStatus.APPROVED, result.getStatus());
        assertEquals(2L, result.getApproverId());
        assertEquals("审批通过", result.getApprovalRemark());
        verify(agentRepository).save(argThat(agent ->
                agent.getStatus() == Agent.AgentStatus.APPROVED
        ));
    }

    @Test
    @DisplayName("审批拒绝 - 成功")
    void testRejectStep() {
        when(agentApprovalRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testApproval));
        when(agentRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testAgent));
        when(agentApprovalRepository.save(any(AgentApproval.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(agentRepository.save(any(Agent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgentApproval result = approvalService.reject(1L, "不符合要求", 2L);

        assertNotNull(result);
        assertEquals(AgentApproval.ApprovalStatus.REJECTED, result.getStatus());
        assertEquals(2L, result.getApproverId());
        assertEquals("不符合要求", result.getApprovalRemark());
        verify(agentRepository).save(argThat(agent ->
                agent.getStatus() == Agent.AgentStatus.DRAFT
        ));
    }

    @Test
    @DisplayName("审批 - 非待审批状态抛出异常")
    void testApproveStep_NotPending() {
        testApproval.setStatus(AgentApproval.ApprovalStatus.APPROVED);
        when(agentApprovalRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testApproval));

        assertThrows(BusinessException.class, () ->
                approvalService.approve(1L, "重复审批", 2L)
        );
    }

    @Test
    @DisplayName("获取审批详情 - 成功")
    void testGetCurrentStep() {
        when(agentApprovalRepository.findByIdAndTenantId(1L, 100L))
                .thenReturn(Optional.of(testApproval));

        AgentApproval result = approvalService.getApprovalById(1L);

        assertNotNull(result);
        assertEquals(AgentApproval.ApprovalStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("获取审批详情 - 不存在抛出异常")
    void testGetCurrentStep_NotFound() {
        when(agentApprovalRepository.findByIdAndTenantId(999L, 100L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                approvalService.getApprovalById(999L)
        );
    }
}
