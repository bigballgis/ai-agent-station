package com.aiagent.service;

import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.entity.WorkflowInstance;
import com.aiagent.entity.WorkflowNodeLog;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.WorkflowDefinitionRepository;
import com.aiagent.repository.WorkflowInstanceRepository;
import com.aiagent.repository.WorkflowNodeLogRepository;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowEngine 单元测试
 * 测试工作流引擎的启动、节点执行、超时处理、状态转换、节点/边数量验证等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工作流引擎测试")
class WorkflowEngineTest {

    @Mock
    private WorkflowDefinitionRepository definitionRepository;

    @Mock
    private WorkflowInstanceRepository instanceRepository;

    @Mock
    private WorkflowNodeLogRepository nodeLogRepository;

    @Mock
    private WorkflowAsyncExecutor workflowAsyncExecutor;

    @InjectMocks
    private WorkflowEngine workflowEngine;

    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    private WorkflowDefinition publishedDefinition;
    private WorkflowDefinition draftDefinition;
    private Map<String, Object> testVariables;
    private static final Long TENANT_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long DEFINITION_ID = 10L;

    @BeforeEach
    void setUp() {
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(TENANT_ID);

        // 设置 maxExecutionDurationSeconds（通过 @Value 注入，Mockito 不会自动设置）
        ReflectionTestUtils.setField(workflowEngine, "maxExecutionDurationSeconds", 300);

        testVariables = new HashMap<>();
        testVariables.put("input", "test_data");

        // 构建已发布的工作流定义（含 START -> AGENT -> END 节点）
        publishedDefinition = buildPublishedDefinition();

        // 构建草稿状态的工作流定义
        draftDefinition = new WorkflowDefinition();
        draftDefinition.setId(20L);
        draftDefinition.setName("草稿工作流");
        draftDefinition.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        draftDefinition.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    // ==================== startWorkflow 测试 ====================

    @Test
    @DisplayName("启动工作流 - 已发布定义，成功创建实例并转为RUNNING")
    void testStartWorkflow_WithPublishedDefinition_Success() {
        when(definitionRepository.findByIdAndTenantId(DEFINITION_ID, TENANT_ID))
                .thenReturn(Optional.of(publishedDefinition));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> {
            WorkflowInstance inst = inv.getArgument(0);
            if (inst.getId() == null) {
                inst.setId(1L);
            }
            return inst;
        });
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance result = workflowEngine.startWorkflow(DEFINITION_ID, testVariables, USER_ID);

        assertNotNull(result);
        assertEquals(WorkflowInstance.InstanceStatus.RUNNING, result.getStatus());
        assertEquals("测试工作流", result.getWorkflowName());
        assertEquals(DEFINITION_ID, result.getWorkflowDefinitionId());
        assertEquals(USER_ID, result.getStartedBy());
        assertEquals(TENANT_ID, result.getTenantId());
        assertNotNull(result.getCurrentNodeId());
        verify(instanceRepository, times(2)).save(any(WorkflowInstance.class));
        verify(workflowAsyncExecutor).executeNodeAsync(anyLong());
    }

    @Test
    @DisplayName("启动工作流 - 定义不存在，抛出ResourceNotFoundException")
    void testStartWorkflow_DefinitionNotFound() {
        when(definitionRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                workflowEngine.startWorkflow(999L, testVariables, USER_ID)
        );
    }

    @Test
    @DisplayName("启动工作流 - 定义未发布，抛出BusinessException")
    void testStartWorkflow_DefinitionNotPublished() {
        when(definitionRepository.findByIdAndTenantId(20L, TENANT_ID))
                .thenReturn(Optional.of(draftDefinition));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                workflowEngine.startWorkflow(20L, testVariables, USER_ID)
        );
        assertTrue(exception.getMessage().contains("未发布"));
    }

    @Test
    @DisplayName("启动工作流 - 无变量传入，使用空Map")
    void testStartWorkflow_NullVariables() {
        when(definitionRepository.findByIdAndTenantId(DEFINITION_ID, TENANT_ID))
                .thenReturn(Optional.of(publishedDefinition));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> {
            WorkflowInstance inst = inv.getArgument(0);
            inst.setId(2L);
            return inst;
        });
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance result = workflowEngine.startWorkflow(DEFINITION_ID, null, USER_ID);

        assertNotNull(result);
        assertNotNull(result.getVariables());
        assertTrue(result.getVariables().isEmpty());
    }

    // ==================== executeNode 测试 ====================

    @Test
    @DisplayName("执行节点 - START节点成功执行并推进到下一节点")
    void testExecuteNode_StartNode_Success() {
        WorkflowInstance instance = buildRunningInstance("node_start");

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(publishedDefinition));
        when(nodeLogRepository.findByInstanceIdAndNodeId(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowNodeLog result = workflowEngine.executeNode(1L);

        assertNotNull(result);
        assertEquals(WorkflowNodeLog.NodeLogStatus.COMPLETED, result.getStatus());
        verify(workflowAsyncExecutor).executeNodeAsync(anyLong());
    }

    @Test
    @DisplayName("执行节点 - END节点完成工作流")
    void testExecuteNode_EndNode_CompletesWorkflow() {
        WorkflowInstance instance = buildRunningInstance("node_end");

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(publishedDefinition));
        when(nodeLogRepository.findByInstanceIdAndNodeId(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowNodeLog result = workflowEngine.executeNode(1L);

        assertNotNull(result);
        verify(instanceRepository).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.COMPLETED
        ));
    }

    @Test
    @DisplayName("执行节点 - APPROVAL节点挂起工作流")
    void testExecuteNode_ApprovalNode_SuspendsWorkflow() {
        WorkflowInstance instance = buildRunningInstance("node_approval");

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(publishedDefinition));
        when(nodeLogRepository.findByInstanceIdAndNodeId(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowNodeLog result = workflowEngine.executeNode(1L);

        assertNotNull(result);
        verify(instanceRepository).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.SUSPENDED
        ));
        verify(workflowAsyncExecutor, never()).executeNodeAsync(anyLong());
    }

    @Test
    @DisplayName("执行节点 - 超时处理，实例转为FAILED")
    void testExecuteNode_TimeoutHandling() {
        // 设置 startedAt 为很久以前，确保超时
        WorkflowInstance instance = buildRunningInstance("node_start");
        instance.setStartedAt(LocalDateTime.now().minusSeconds(600));

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                workflowEngine.executeNode(1L)
        );
        assertTrue(exception.getMessage().contains("超时"));
        verify(instanceRepository).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.FAILED
                        && inst.getError() != null
                        && inst.getError().contains("超时")
        ));
    }

    @Test
    @DisplayName("执行节点 - 实例不在运行状态，抛出BusinessException")
    void testExecuteNode_NotRunningStatus() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setStatus(WorkflowInstance.InstanceStatus.COMPLETED);

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThrows(BusinessException.class, () ->
                workflowEngine.executeNode(1L)
        );
    }

    // ==================== 状态转换测试 ====================

    @Test
    @DisplayName("工作流状态转换 - PENDING -> RUNNING -> COMPLETED")
    void testWorkflowStateTransition_PendingToRunningToCompleted() {
        // startWorkflow: PENDING -> RUNNING
        when(definitionRepository.findByIdAndTenantId(DEFINITION_ID, TENANT_ID))
                .thenReturn(Optional.of(publishedDefinition));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> {
            WorkflowInstance inst = inv.getArgument(0);
            if (inst.getId() == null) inst.setId(1L);
            return inst;
        });
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance started = workflowEngine.startWorkflow(DEFINITION_ID, testVariables, USER_ID);
        assertEquals(WorkflowInstance.InstanceStatus.RUNNING, started.getStatus());

        // executeNode (END): RUNNING -> COMPLETED
        started.setCurrentNodeId("node_end");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(started));
        when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(publishedDefinition));
        when(nodeLogRepository.findByInstanceIdAndNodeId(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());

        workflowEngine.executeNode(1L);

        verify(instanceRepository, atLeastOnce()).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.COMPLETED
        ));
    }

    @Test
    @DisplayName("工作流状态转换 - RUNNING -> FAILED (节点执行失败)")
    void testWorkflowStateTransition_RunningToFailed() {
        WorkflowInstance instance = buildRunningInstance("node_start");
        instance.setStartedAt(LocalDateTime.now().minusSeconds(600));

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThrows(BusinessException.class, () -> workflowEngine.executeNode(1L));

        verify(instanceRepository).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.FAILED
        ));
    }

    // ==================== 节点数量验证测试 ====================

    @Test
    @DisplayName("节点数量验证 - 定义无节点配置，启动时抛出异常")
    void testNodeCountValidation_NoNodesConfigured() {
        WorkflowDefinition noNodesDef = new WorkflowDefinition();
        noNodesDef.setId(30L);
        noNodesDef.setName("无节点工作流");
        noNodesDef.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        noNodesDef.setTenantId(TENANT_ID);
        noNodesDef.setNodes(null);

        when(definitionRepository.findByIdAndTenantId(30L, TENANT_ID))
                .thenReturn(Optional.of(noNodesDef));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                workflowEngine.startWorkflow(30L, testVariables, USER_ID)
        );
        assertTrue(exception.getMessage().contains("没有节点配置"));
    }

    @Test
    @DisplayName("节点数量验证 - 有节点但无START节点，启动时抛出异常")
    void testNodeCountValidation_NoStartNode() {
        WorkflowDefinition noStartDef = new WorkflowDefinition();
        noStartDef.setId(40L);
        noStartDef.setName("无开始节点工作流");
        noStartDef.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        noStartDef.setTenantId(TENANT_ID);

        Map<String, Object> nodesMap = new HashMap<>();
        List<Map<String, Object>> nodeList = new ArrayList<>();
        Map<String, Object> agentNode = new HashMap<>();
        agentNode.put("id", "node_agent");
        agentNode.put("type", "AGENT");
        agentNode.put("name", "Agent节点");
        nodeList.add(agentNode);
        nodesMap.put("nodes", nodeList);
        noStartDef.setNodes(nodesMap);

        when(definitionRepository.findByIdAndTenantId(40L, TENANT_ID))
                .thenReturn(Optional.of(noStartDef));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                workflowEngine.startWorkflow(40L, testVariables, USER_ID)
        );
        assertTrue(exception.getMessage().contains("没有开始节点"));
    }

    // ==================== 边数量验证测试 ====================

    @Test
    @DisplayName("边数量验证 - 无边配置时节点执行后直接完成")
    void testEdgeCountValidation_NoEdges() {
        WorkflowDefinition noEdgesDef = buildDefinitionWithNodesOnly();
        WorkflowInstance instance = buildRunningInstance("node_start");
        instance.setWorkflowDefinitionId(noEdgesDef.getId());

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(definitionRepository.findById(noEdgesDef.getId())).thenReturn(Optional.of(noEdgesDef));
        when(nodeLogRepository.findByInstanceIdAndNodeId(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // START 节点执行后，没有边则无法找到下一节点，工作流完成
        workflowEngine.executeNode(1L);

        verify(instanceRepository).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.COMPLETED
        ));
    }

    @Test
    @DisplayName("边数量验证 - 边配置格式错误，节点执行后完成")
    void testEdgeCountValidation_InvalidEdgeFormat() {
        WorkflowDefinition badEdgesDef = buildPublishedDefinition();
        Map<String, Object> badEdges = new HashMap<>();
        badEdges.put("edges", "not_a_list");
        badEdgesDef.setEdges(badEdges);

        WorkflowInstance instance = buildRunningInstance("node_start");
        instance.setWorkflowDefinitionId(badEdgesDef.getId());

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(definitionRepository.findById(badEdgesDef.getId())).thenReturn(Optional.of(badEdgesDef));
        when(nodeLogRepository.findByInstanceIdAndNodeId(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        workflowEngine.executeNode(1L);

        verify(instanceRepository).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.COMPLETED
        ));
    }

    // ==================== cancelWorkflow 测试 ====================

    @Test
    @DisplayName("取消工作流 - 运行中的实例成功取消")
    void testCancelWorkflow_Success() {
        WorkflowInstance instance = buildRunningInstance("node_start");

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance result = workflowEngine.cancelWorkflow(1L, "用户手动取消");

        assertNotNull(result);
        assertEquals(WorkflowInstance.InstanceStatus.CANCELLED, result.getStatus());
        assertEquals("用户手动取消", result.getError());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    @DisplayName("取消工作流 - 已完成的实例无法取消")
    void testCancelWorkflow_AlreadyCompleted() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setStatus(WorkflowInstance.InstanceStatus.COMPLETED);

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThrows(BusinessException.class, () ->
                workflowEngine.cancelWorkflow(1L, "尝试取消")
        );
    }

    // ==================== approveNode 测试 ====================

    @Test
    @DisplayName("审批节点 - 审批通过后恢复工作流")
    void testApproveNode_Approved() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setStatus(WorkflowInstance.InstanceStatus.SUSPENDED);
        instance.setCurrentNodeId("node_approval");
        instance.setWorkflowDefinitionId(DEFINITION_ID);
        instance.setCurrentStep(0);

        WorkflowNodeLog existingLog = new WorkflowNodeLog();
        existingLog.setId(10L);
        existingLog.setStartedAt(LocalDateTime.now());

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(publishedDefinition));
        when(nodeLogRepository.findByInstanceIdAndNodeId(1L, "node_approval"))
                .thenReturn(List.of(existingLog));
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowNodeLog result = workflowEngine.approveNode(1L, true, "同意");

        assertNotNull(result);
        assertEquals(WorkflowNodeLog.NodeLogStatus.COMPLETED, result.getStatus());
        verify(instanceRepository).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.RUNNING
        ));
    }

    @Test
    @DisplayName("审批节点 - 审批拒绝后工作流失败")
    void testApproveNode_Rejected() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setStatus(WorkflowInstance.InstanceStatus.SUSPENDED);
        instance.setCurrentNodeId("node_approval");
        instance.setWorkflowDefinitionId(DEFINITION_ID);

        WorkflowNodeLog existingLog = new WorkflowNodeLog();
        existingLog.setId(10L);
        existingLog.setStartedAt(LocalDateTime.now());

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(definitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(publishedDefinition));
        when(nodeLogRepository.findByInstanceIdAndNodeId(1L, "node_approval"))
                .thenReturn(List.of(existingLog));
        when(nodeLogRepository.save(any(WorkflowNodeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowNodeLog result = workflowEngine.approveNode(1L, false, "不符合要求");

        assertNotNull(result);
        assertEquals(WorkflowNodeLog.NodeLogStatus.FAILED, result.getStatus());
        verify(instanceRepository).save(argThat(inst ->
                inst.getStatus() == WorkflowInstance.InstanceStatus.FAILED
                        && inst.getError().contains("审批被拒绝")
        ));
    }

    // ==================== getWorkflowStatus / getWorkflowHistory 测试 ====================

    @Test
    @DisplayName("获取工作流状态 - 实例存在时返回")
    void testGetWorkflowStatus_Found() {
        WorkflowInstance instance = buildRunningInstance("node_start");

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        WorkflowInstance result = workflowEngine.getWorkflowStatus(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("获取工作流状态 - 实例不存在时抛出异常")
    void testGetWorkflowStatus_NotFound() {
        when(instanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                workflowEngine.getWorkflowStatus(999L)
        );
    }

    @Test
    @DisplayName("获取工作流历史 - 返回节点日志列表")
    void testGetWorkflowHistory() {
        WorkflowNodeLog log1 = new WorkflowNodeLog();
        log1.setNodeId("node_start");
        WorkflowNodeLog log2 = new WorkflowNodeLog();
        log2.setNodeId("node_agent");

        when(nodeLogRepository.findByInstanceIdOrderByStartedAtAsc(1L))
                .thenReturn(List.of(log1, log2));

        List<WorkflowNodeLog> result = workflowEngine.getWorkflowHistory(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ==================== resumeWorkflow 测试 ====================

    @Test
    @DisplayName("恢复工作流 - RUNNING状态实例成功恢复")
    void testResumeWorkflow_Success() {
        WorkflowInstance instance = buildRunningInstance("node_agent");

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        WorkflowInstance result = workflowEngine.resumeWorkflow(1L);

        assertNotNull(result);
        verify(workflowAsyncExecutor).executeNodeAsync(1L);
    }

    @Test
    @DisplayName("恢复工作流 - COMPLETED状态实例无法恢复")
    void testResumeWorkflow_AlreadyCompleted() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setStatus(WorkflowInstance.InstanceStatus.COMPLETED);
        instance.setCurrentNodeId("node_agent");

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThrows(BusinessException.class, () ->
                workflowEngine.resumeWorkflow(1L)
        );
    }

    // ==================== 辅助方法 ====================

    private WorkflowDefinition buildPublishedDefinition() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(DEFINITION_ID);
        def.setName("测试工作流");
        def.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        def.setTenantId(TENANT_ID);

        // 构建节点列表
        Map<String, Object> startNode = new HashMap<>();
        startNode.put("id", "node_start");
        startNode.put("type", "START");
        startNode.put("name", "开始节点");

        Map<String, Object> agentNode = new HashMap<>();
        agentNode.put("id", "node_agent");
        agentNode.put("type", "AGENT");
        agentNode.put("name", "Agent节点");
        agentNode.put("agentId", "agent_001");
        agentNode.put("prompt", "处理输入");

        Map<String, Object> approvalNode = new HashMap<>();
        approvalNode.put("id", "node_approval");
        approvalNode.put("type", "APPROVAL");
        approvalNode.put("name", "审批节点");

        Map<String, Object> endNode = new HashMap<>();
        endNode.put("id", "node_end");
        endNode.put("type", "END");
        endNode.put("name", "结束节点");

        List<Map<String, Object>> nodeList = List.of(startNode, agentNode, approvalNode, endNode);
        Map<String, Object> nodesMap = new HashMap<>();
        nodesMap.put("nodes", nodeList);
        def.setNodes(nodesMap);

        // 构建边列表
        Map<String, Object> edge1 = new HashMap<>();
        edge1.put("source", "node_start");
        edge1.put("target", "node_agent");

        Map<String, Object> edge2 = new HashMap<>();
        edge2.put("source", "node_agent");
        edge2.put("target", "node_approval");

        Map<String, Object> edge3 = new HashMap<>();
        edge3.put("source", "node_approval");
        edge3.put("target", "node_end");

        List<Map<String, Object>> edgeList = List.of(edge1, edge2, edge3);
        Map<String, Object> edgesMap = new HashMap<>();
        edgesMap.put("edges", edgeList);
        def.setEdges(edgesMap);

        return def;
    }

    private WorkflowDefinition buildDefinitionWithNodesOnly() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(50L);
        def.setName("仅节点工作流");
        def.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        def.setTenantId(TENANT_ID);

        Map<String, Object> startNode = new HashMap<>();
        startNode.put("id", "node_start");
        startNode.put("type", "START");
        startNode.put("name", "开始节点");

        Map<String, Object> endNode = new HashMap<>();
        endNode.put("id", "node_end");
        endNode.put("type", "END");
        endNode.put("name", "结束节点");

        List<Map<String, Object>> nodeList = List.of(startNode, endNode);
        Map<String, Object> nodesMap = new HashMap<>();
        nodesMap.put("nodes", nodeList);
        def.setNodes(nodesMap);
        // edges 为 null

        return def;
    }

    private WorkflowInstance buildRunningInstance(String currentNodeId) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setWorkflowDefinitionId(DEFINITION_ID);
        instance.setWorkflowName("测试工作流");
        instance.setStatus(WorkflowInstance.InstanceStatus.RUNNING);
        instance.setCurrentNodeId(currentNodeId);
        instance.setCurrentStep(0);
        instance.setVariables(testVariables);
        instance.setStartedBy(USER_ID);
        instance.setStartedAt(LocalDateTime.now());
        instance.setTenantId(TENANT_ID);
        return instance;
    }
}
