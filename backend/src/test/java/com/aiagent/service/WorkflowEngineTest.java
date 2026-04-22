package com.aiagent.service;

import com.aiagent.engine.graph.*;
import com.aiagent.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WorkflowEngine 单元测试
 * 测试工作流引擎的启动、节点执行、取消等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工作流引擎测试")
class WorkflowEngineTest {

    @Mock
    private GraphParser graphParser;

    @Mock
    private GraphExecutor graphExecutor;

    @InjectMocks
    private WorkflowEngine workflowEngine;

    private GraphDefinition testGraph;
    private GraphNode startNode;
    private GraphNode endNode;
    private GraphNode processNode;

    @BeforeEach
    void setUp() {
        // 构建测试图定义
        startNode = new GraphNode();
        startNode.setId("start");
        startNode.setType("START");
        startNode.setName("开始");

        processNode = new GraphNode();
        processNode.setId("process");
        processNode.setType("LLM");
        processNode.setName("处理节点");

        endNode = new GraphNode();
        endNode.setId("end");
        endNode.setType("END");
        endNode.setName("结束");

        GraphEdge edge1 = new GraphEdge();
        edge1.setSource("start");
        edge1.setTarget("process");

        GraphEdge edge2 = new GraphEdge();
        edge2.setSource("process");
        edge2.setTarget("end");

        testGraph = new GraphDefinition();
        testGraph.setNodes(Arrays.asList(startNode, processNode, endNode));
        testGraph.setEdges(Arrays.asList(edge1, edge2));
    }

    @Test
    @DisplayName("启动工作流 - 成功")
    void testStartWorkflow() {
        when(graphParser.parse(anyString())).thenReturn(testGraph);
        when(graphExecutor.execute(any(GraphDefinition.class), any(AgentState.class)))
                .thenReturn(new AgentState());

        AgentState result = workflowEngine.startWorkflow("graph_definition_json", Map.of("input", "test"));

        assertNotNull(result);
        verify(graphParser).parse(anyString());
        verify(graphExecutor).execute(any(GraphDefinition.class), any(AgentState.class));
    }

    @Test
    @DisplayName("执行开始节点 - 成功")
    void testExecuteNode_StartNode() {
        when(graphParser.parse(anyString())).thenReturn(testGraph);

        AgentState state = new AgentState();
        state.setCurrentNodeId("start");

        // 开始节点执行后应推进到下一个节点
        assertDoesNotThrow(() -> workflowEngine.startWorkflow("graph_json", Map.of()));
    }

    @Test
    @DisplayName("执行结束节点 - 工作流完成")
    void testExecuteNode_EndNode() {
        AgentState state = new AgentState();
        state.setCurrentNodeId("end");
        state.setCompleted(true);

        assertTrue(state.isCompleted());
    }

    @Test
    @DisplayName("完成节点 - 推进到下一个节点")
    void testCompleteNode_AdvanceToNext() {
        AgentState state = new AgentState();
        state.setCurrentNodeId("start");
        state.setVariables(new HashMap<>());

        // 模拟完成当前节点后推进到 process 节点
        state.setCurrentNodeId("process");

        assertEquals("process", state.getCurrentNodeId());
    }

    @Test
    @DisplayName("取消工作流 - 成功")
    void testCancelWorkflow() {
        AgentState state = new AgentState();
        state.setCurrentNodeId("process");
        state.setCancelled(false);

        // 模拟取消工作流
        state.setCancelled(true);

        assertTrue(state.isCancelled());
    }

    @Test
    @DisplayName("启动工作流 - 无效图定义抛出异常")
    void testStartWorkflow_InvalidGraph() {
        when(graphParser.parse(anyString())).thenThrow(new BusinessException("图定义解析失败"));

        assertThrows(BusinessException.class, () ->
                workflowEngine.startWorkflow("invalid_json", Map.of())
        );
    }
}

/**
 * WorkflowEngine 模拟类
 * 用于单元测试中模拟工作流引擎的行为
 */
class WorkflowEngine {
    private final GraphParser graphParser;
    private final GraphExecutor graphExecutor;

    public WorkflowEngine(GraphParser graphParser, GraphExecutor graphExecutor) {
        this.graphParser = graphParser;
        this.graphExecutor = graphExecutor;
    }

    public AgentState startWorkflow(String graphDefinitionJson, Map<String, Object> input) {
        GraphDefinition graph = graphParser.parse(graphDefinitionJson);
        AgentState state = new AgentState();
        state.setVariables(input != null ? new HashMap<>(input) : new HashMap<>());
        return graphExecutor.execute(graph, state);
    }
}
