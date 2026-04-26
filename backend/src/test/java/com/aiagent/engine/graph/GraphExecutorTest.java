package com.aiagent.engine.graph;

import com.aiagent.exception.BusinessException;
import com.aiagent.mcp.McpToolGateway;
import com.aiagent.service.MemoryService;
import com.aiagent.service.llm.LangChain4jService;
import com.aiagent.service.tool.CompositeToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GraphExecutor 单元测试
 * 覆盖核心执行引擎的关键路径
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GraphExecutor 执行引擎测试")
class GraphExecutorTest {

    @Mock
    private LangChain4jService langChain4jService;

    @Mock
    private CompositeToolProvider compositeToolProvider;

    @Mock
    private McpToolGateway mcpToolGateway;

    @Mock
    private MemoryService memoryService;

    @Mock
    private HttpExecutor httpExecutor;

    @Mock
    private NodeExecutors nodeExecutors;

    /** Same-thread executor for deterministic unit tests */
    private final Executor directExecutor = r -> r.run();

    private GraphDefinition graphDefinition;
    private GraphExecutor executor;

    @BeforeEach
    void setUp() {
        graphDefinition = new GraphDefinition();
        executor = new GraphExecutor(
                langChain4jService,
                compositeToolProvider,
                mcpToolGateway,
                memoryService,
                httpExecutor,
                nodeExecutors,
                directExecutor);
    }

    // Helper to create a graph node
    private GraphNode createNode(String id, String type, String label) {
        GraphNode node = new GraphNode();
        node.setId(id);
        node.setType(type);
        node.setLabel(label);
        node.setConfig(new HashMap<>());
        node.setInputPorts(new ArrayList<>());
        node.setOutputPorts(new ArrayList<>());
        return node;
    }

    // Helper to create an edge
    private GraphEdge createEdge(String sourceId, String sourcePort, String targetId, String targetPort) {
        GraphEdge edge = new GraphEdge();
        edge.setSourceNodeId(sourceId);
        edge.setSourcePort(sourcePort);
        edge.setTargetNodeId(targetId);
        edge.setTargetPort(targetPort);
        return edge;
    }

    // Helper to build a valid graph definition from nodes and edges
    private void buildGraph(List<GraphNode> nodes, List<GraphEdge> edges, String entryNodeId) {
        Map<String, GraphNode> nodeMap = new LinkedHashMap<>();
        for (GraphNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        graphDefinition.setNodes(nodeMap);
        graphDefinition.setEdges(edges != null ? edges : new ArrayList<>());
        graphDefinition.setEntryNodeId(entryNodeId);
    }

    // Helper to create a fresh AgentState
    private AgentState createInitialState() {
        AgentState state = new AgentState();
        state.setInputs(new HashMap<>());
        state.setVariables(new HashMap<>());
        state.setProcessedInput(new HashMap<>());
        state.setOutputs(new HashMap<>());
        state.setToolResults(new HashMap<>());
        return state;
    }

    @Test
    @DisplayName("空图执行应抛出验证异常")
    void testEmptyGraph() {
        // No nodes, no entry node
        graphDefinition.setNodes(new HashMap<>());
        graphDefinition.setEdges(new ArrayList<>());
        graphDefinition.setEntryNodeId(null);

        AgentState state = createInitialState();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> executor.execute(graphDefinition, state));
        assertTrue(exception.getMessage().contains("图定义验证失败"));
    }

    @Test
    @DisplayName("入口节点不存在应抛出验证异常")
    void testMissingEntryNode() {
        graphDefinition.setNodes(new HashMap<>());
        graphDefinition.setEdges(new ArrayList<>());
        graphDefinition.setEntryNodeId("nonexistent");

        AgentState state = createInitialState();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> executor.execute(graphDefinition, state));
        assertTrue(exception.getMessage().contains("图定义验证失败"));
    }

    @Test
    @DisplayName("仅Start节点应正常完成")
    void testStartNodeOnly() {
        GraphNode start = createNode("start_1", "start", "开始");
        buildGraph(List.of(start), new ArrayList<>(), "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", start.getStatus());
    }

    @Test
    @DisplayName("Start -> End 线性流程应正常完成")
    void testLinearFlow() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(createEdge("start_1", "output", "end_1", "input"));
        buildGraph(List.of(start, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", start.getStatus());
        assertEquals("completed", end.getStatus());
    }

    @Test
    @DisplayName("条件节点 - 表达式为真时应路由到true分支")
    void testConditionRouting_TrueBranch() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode condition = createNode("cond_1", "condition", "条件判断");
        condition.getConfig().put("expression", "score > 50");
        condition.getInputPorts().add("input");
        condition.getOutputPorts().add("true");
        condition.getOutputPorts().add("false");

        GraphNode endTrue = createNode("end_true", "end", "真分支");
        endTrue.getInputPorts().add("input");

        GraphNode endFalse = createNode("end_false", "end", "假分支");
        endFalse.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "cond_1", "input"),
                createEdge("cond_1", "true", "end_true", "input"),
                createEdge("cond_1", "false", "end_false", "input")
        );
        buildGraph(List.of(start, condition, endTrue, endFalse), edges, "start_1");

        AgentState state = createInitialState();
        state.setVariable("score", "80");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", endTrue.getStatus());
        // The false branch should not have been executed (no status set beyond default)
        // endFalse should remain "pending" since it was never reached
        assertEquals("pending", endFalse.getStatus());
    }

    @Test
    @DisplayName("条件节点 - 表达式为假时应路由到false分支")
    void testConditionRouting_FalseBranch() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode condition = createNode("cond_1", "condition", "条件判断");
        condition.getConfig().put("expression", "score > 50");
        condition.getInputPorts().add("input");
        condition.getOutputPorts().add("true");
        condition.getOutputPorts().add("false");

        GraphNode endTrue = createNode("end_true", "end", "真分支");
        endTrue.getInputPorts().add("input");

        GraphNode endFalse = createNode("end_false", "end", "假分支");
        endFalse.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "cond_1", "input"),
                createEdge("cond_1", "true", "end_true", "input"),
                createEdge("cond_1", "false", "end_false", "input")
        );
        buildGraph(List.of(start, condition, endTrue, endFalse), edges, "start_1");

        AgentState state = createInitialState();
        state.setVariable("score", "20");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", endFalse.getStatus());
        assertEquals("pending", endTrue.getStatus());
    }

    @Test
    @DisplayName("条件节点 - 基于变量布尔值判断")
    void testConditionRouting_VariableBased() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode condition = createNode("cond_1", "condition", "条件判断");
        condition.getConfig().put("variable", "isEnabled");
        condition.getInputPorts().add("input");
        condition.getOutputPorts().add("true");
        condition.getOutputPorts().add("false");

        GraphNode endTrue = createNode("end_true", "end", "真分支");
        endTrue.getInputPorts().add("input");

        GraphNode endFalse = createNode("end_false", "end", "假分支");
        endFalse.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "cond_1", "input"),
                createEdge("cond_1", "true", "end_true", "input"),
                createEdge("cond_1", "false", "end_false", "input")
        );
        buildGraph(List.of(start, condition, endTrue, endFalse), edges, "start_1");

        AgentState state = createInitialState();
        state.setVariable("isEnabled", "true");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", endTrue.getStatus());
    }

    @Test
    @DisplayName("变量节点应正确设置状态变量")
    void testVariableNode() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode variable = createNode("var_1", "variable", "设置变量");
        variable.getConfig().put("name", "myVar");
        variable.getConfig().put("value", "hello");
        variable.getInputPorts().add("input");
        variable.getOutputPorts().add("output");

        GraphNode end = createNode("end_1", "end", "结束");
        end.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "var_1", "input"),
                createEdge("var_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, variable, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("hello", state.getVariables().get("myVar"));
        assertEquals("hello", variable.getOutput());
    }

    @Test
    @DisplayName("变量节点 - 从source获取值")
    void testVariableNode_FromSource() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode var1 = createNode("var_1", "variable", "设置变量1");
        var1.getConfig().put("name", "greeting");
        var1.getConfig().put("value", "你好世界");

        GraphNode var2 = createNode("var_2", "variable", "引用变量1");
        var2.getConfig().put("name", "copy");
        var2.getConfig().put("source", "greeting");

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "var_1", "input"),
                createEdge("var_1", "output", "var_2", "input"),
                createEdge("var_2", "output", "end_1", "input")
        );
        buildGraph(List.of(start, var1, var2, end), edges, "start_1");

        AgentState state = createInitialState();
        executor.execute(graphDefinition, state);

        assertEquals("你好世界", state.getVariable("greeting"));
        assertEquals("你好世界", state.getVariable("copy"));
    }

    @Test
    @DisplayName("异常节点 - fallback动作应设置降级值")
    void testExceptionHandler_Fallback() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode exception = createNode("exc_1", "exception", "异常处理");
        exception.getConfig().put("action", "fallback");
        exception.getConfig().put("fallbackValue", "default_result");
        exception.getInputPorts().add("input");
        exception.getOutputPorts().add("output");

        GraphNode end = createNode("end_1", "end", "结束");
        end.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "exc_1", "input"),
                createEdge("exc_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, exception, end), edges, "start_1");

        AgentState state = createInitialState();
        state.setError("模拟错误");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("default_result", state.getLlmOutput());
        assertNull(state.getError()); // error should be cleared
    }

    @Test
    @DisplayName("异常节点 - retry动作应清除错误")
    void testExceptionHandler_Retry() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode exception = createNode("exc_1", "exception", "异常处理");
        exception.getConfig().put("action", "retry");
        exception.getInputPorts().add("input");
        exception.getOutputPorts().add("output");

        GraphNode end = createNode("end_1", "end", "结束");
        end.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "exc_1", "input"),
                createEdge("exc_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, exception, end), edges, "start_1");

        AgentState state = createInitialState();
        state.setError("模拟错误");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertNull(state.getError()); // error should be cleared by retry
    }

    @Test
    @DisplayName("节点执行失败且无异常处理节点应终止执行")
    void testNodeFailure_NoExceptionHandler() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode code = createNode("code_1", "code", "代码节点");
        code.getConfig().put("language", "javascript");
        code.getConfig().put("code", "throw new Error('test error')");
        code.getInputPorts().add("input");
        code.getOutputPorts().add("output");

        GraphNode end = createNode("end_1", "end", "结束");
        end.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "code_1", "input"),
                createEdge("code_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, code, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("failed", result.get("status"));
        assertTrue(result.containsKey("error"));
        assertEquals("failed", code.getStatus());
        // End node should not have been reached
        assertEquals("pending", end.getStatus());
    }

    @Test
    @DisplayName("节点执行失败且有异常处理节点应继续执行")
    void testNodeFailure_WithExceptionHandler() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode code = createNode("code_1", "code", "代码节点");
        code.getConfig().put("language", "javascript");
        code.getConfig().put("code", "throw new Error('test error')");

        GraphNode exception = createNode("exc_1", "exception", "异常处理");
        exception.getConfig().put("action", "fallback");
        exception.getConfig().put("fallbackValue", "recovered");

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "code_1", "input"),
                createEdge("code_1", "output", "exc_1", "input"),
                createEdge("exc_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, code, exception, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        // After exception handler clears error and sets fallback, execution continues to end
        assertEquals("completed", end.getStatus());
    }

    @Test
    @DisplayName("延迟节点应等待指定秒数")
    void testDelayNode() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode delay = createNode("delay_1", "delay", "延迟");
        delay.getConfig().put("seconds", 1);
        delay.getInputPorts().add("input");
        delay.getOutputPorts().add("output");

        GraphNode end = createNode("end_1", "end", "结束");
        end.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "delay_1", "input"),
                createEdge("delay_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, delay, end), edges, "start_1");

        AgentState state = createInitialState();
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = executor.execute(graphDefinition, state);
        long elapsed = System.currentTimeMillis() - startTime;

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        // Should have waited at least 1 second (allow 200ms tolerance)
        assertTrue(elapsed >= 800, "Delay node should wait at least 1 second, but only waited " + elapsed + "ms");
        assertEquals("delayed_1s", delay.getOutput());
    }

    @Test
    @DisplayName("延迟节点 - 秒数为0时应使用最小值1")
    void testDelayNode_MinimumOneSecond() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode delay = createNode("delay_1", "delay", "延迟");
        delay.getConfig().put("seconds", 0);

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "delay_1", "input"),
                createEdge("delay_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, delay, end), edges, "start_1");

        AgentState state = createInitialState();
        long startTime = System.currentTimeMillis();
        executor.execute(graphDefinition, state);
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(elapsed >= 800, "Delay with 0 seconds should use minimum of 1 second");
        assertEquals("delayed_1s", delay.getOutput());
    }

    @Test
    @DisplayName("合并节点 - append策略应合并多个分支输出")
    void testMergeNode_AppendStrategy() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode var1 = createNode("var_1", "variable", "变量1");
        var1.getConfig().put("name", "a");
        var1.getConfig().put("value", "结果A");

        GraphNode var2 = createNode("var_2", "variable", "变量2");
        var2.getConfig().put("name", "b");
        var2.getConfig().put("value", "结果B");

        GraphNode merge = createNode("merge_1", "merge", "合并");
        merge.getConfig().put("mergeStrategy", "append");

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "var_1", "input"),
                createEdge("start_1", "output", "var_2", "input"),
                createEdge("var_1", "output", "merge_1", "input1"),
                createEdge("var_2", "output", "merge_1", "input2"),
                createEdge("merge_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, var1, var2, merge, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        // Merge node should have combined output from both branches
        String mergeOutput = merge.getOutput();
        assertNotNull(mergeOutput);
        assertTrue(mergeOutput.contains("结果A"));
        assertTrue(mergeOutput.contains("结果B"));
    }

    @Test
    @DisplayName("合并节点 - first策略应取第一个输入")
    void testMergeNode_FirstStrategy() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode var1 = createNode("var_1", "variable", "变量1");
        var1.getConfig().put("name", "a");
        var1.getConfig().put("value", "第一个");

        GraphNode var2 = createNode("var_2", "variable", "变量2");
        var2.getConfig().put("name", "b");
        var2.getConfig().put("value", "第二个");

        GraphNode merge = createNode("merge_1", "merge", "合并");
        merge.getConfig().put("mergeStrategy", "first");

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "var_1", "input"),
                createEdge("start_1", "output", "var_2", "input"),
                createEdge("var_1", "output", "merge_1", "input1"),
                createEdge("var_2", "output", "merge_1", "input2"),
                createEdge("merge_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, var1, var2, merge, end), edges, "start_1");

        AgentState state = createInitialState();
        executor.execute(graphDefinition, state);

        assertEquals("第一个", merge.getOutput());
    }

    @Test
    @DisplayName("Switch节点 - 应路由到匹配的分支")
    void testSwitchNode() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode switchNode = createNode("switch_1", "switch", "路由");
        List<Map<String, String>> cases = new ArrayList<>();
        cases.add(Map.of("expression", "type == premium", "outputPort", "premium"));
        cases.add(Map.of("expression", "type == basic", "outputPort", "basic"));
        switchNode.getConfig().put("cases", cases);
        switchNode.getInputPorts().add("input");
        switchNode.getOutputPorts().add("premium");
        switchNode.getOutputPorts().add("basic");
        switchNode.getOutputPorts().add("default");

        GraphNode endPremium = createNode("end_premium", "end", "高级");
        endPremium.getInputPorts().add("input");

        GraphNode endBasic = createNode("end_basic", "end", "基础");
        endBasic.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "switch_1", "input"),
                createEdge("switch_1", "premium", "end_premium", "input"),
                createEdge("switch_1", "basic", "end_basic", "input")
        );
        buildGraph(List.of(start, switchNode, endPremium, endBasic), edges, "start_1");

        AgentState state = createInitialState();
        state.setVariable("type", "premium");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", endPremium.getStatus());
        assertEquals("pending", endBasic.getStatus());
    }

    @Test
    @DisplayName("Switch节点 - 无匹配时使用default分支")
    void testSwitchNode_DefaultBranch() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode switchNode = createNode("switch_1", "switch", "路由");
        List<Map<String, String>> cases = new ArrayList<>();
        cases.add(Map.of("expression", "type == premium", "outputPort", "premium"));
        switchNode.getConfig().put("cases", cases);
        switchNode.getInputPorts().add("input");
        switchNode.getOutputPorts().add("premium");
        switchNode.getOutputPorts().add("default");

        GraphNode endDefault = createNode("end_default", "end", "默认");
        endDefault.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "switch_1", "input"),
                createEdge("switch_1", "default", "end_default", "input")
        );
        buildGraph(List.of(start, switchNode, endDefault), edges, "start_1");

        AgentState state = createInitialState();
        state.setVariable("type", "unknown");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", endDefault.getStatus());
    }

    @Test
    @DisplayName("最大迭代次数应防止无限循环")
    void testMaxIterationsGuard() {
        // Create a graph with many sequential nodes to consume iterations
        // The default maxIterations is 25, so we create more than 25 nodes
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        GraphNode start = createNode("start_1", "start", "开始");
        nodes.add(start);

        // Create 30 variable nodes (more than maxIterations=25)
        for (int i = 0; i < 30; i++) {
            GraphNode var = createNode("var_" + i, "variable", "变量" + i);
            var.getConfig().put("name", "v" + i);
            var.getConfig().put("value", "val" + i);
            nodes.add(var);

            String sourceId = (i == 0) ? "start_1" : "var_" + (i - 1);
            edges.add(createEdge(sourceId, "output", "var_" + i, "input"));
        }

        buildGraph(nodes, edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("failed", result.get("status"));
        assertTrue(result.containsKey("error"));
        assertTrue(((String) result.get("error")).contains("最大迭代次数"));
    }

    @Test
    @DisplayName("表达式求值 - equals操作符")
    void testExpressionEvaluation_Equals() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode condition = createNode("cond_1", "condition", "条件判断");
        condition.getConfig().put("expression", "color == red");
        condition.getInputPorts().add("input");
        condition.getOutputPorts().add("true");
        condition.getOutputPorts().add("false");

        GraphNode endTrue = createNode("end_true", "end", "真分支");
        endTrue.getInputPorts().add("input");

        GraphNode endFalse = createNode("end_false", "end", "假分支");
        endFalse.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "cond_1", "input"),
                createEdge("cond_1", "true", "end_true", "input"),
                createEdge("cond_1", "false", "end_false", "input")
        );
        buildGraph(List.of(start, condition, endTrue, endFalse), edges, "start_1");

        AgentState state = createInitialState();
        state.setVariable("color", "red");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertEquals("success", result.get("status"));
        assertEquals("completed", endTrue.getStatus());
    }

    @Test
    @DisplayName("表达式求值 - contains操作符")
    void testExpressionEvaluation_Contains() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode condition = createNode("cond_1", "condition", "条件判断");
        condition.getConfig().put("expression", "text contains hello");
        condition.getInputPorts().add("input");
        condition.getOutputPorts().add("true");
        condition.getOutputPorts().add("false");

        GraphNode endTrue = createNode("end_true", "end", "真分支");
        endTrue.getInputPorts().add("input");

        GraphNode endFalse = createNode("end_false", "end", "假分支");
        endFalse.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "cond_1", "input"),
                createEdge("cond_1", "true", "end_true", "input"),
                createEdge("cond_1", "false", "end_false", "input")
        );
        buildGraph(List.of(start, condition, endTrue, endFalse), edges, "start_1");

        AgentState state = createInitialState();
        state.setVariable("text", "say hello world");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertEquals("success", result.get("status"));
        assertEquals("completed", endTrue.getStatus());
    }

    @Test
    @DisplayName("表达式求值 - not equals操作符")
    void testExpressionEvaluation_NotEquals() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode condition = createNode("cond_1", "condition", "条件判断");
        condition.getConfig().put("expression", "color != blue");
        condition.getInputPorts().add("input");
        condition.getOutputPorts().add("true");
        condition.getOutputPorts().add("false");

        GraphNode endTrue = createNode("end_true", "end", "真分支");
        endTrue.getInputPorts().add("input");

        GraphNode endFalse = createNode("end_false", "end", "假分支");
        endFalse.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "cond_1", "input"),
                createEdge("cond_1", "true", "end_true", "input"),
                createEdge("cond_1", "false", "end_false", "input")
        );
        buildGraph(List.of(start, condition, endTrue, endFalse), edges, "start_1");

        AgentState state = createInitialState();
        state.setVariable("color", "red");

        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertEquals("success", result.get("status"));
        assertEquals("completed", endTrue.getStatus());
    }

    @Test
    @DisplayName("Start节点应将输入复制到processedInput")
    void testStartNode_CopiesInputs() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(createEdge("start_1", "output", "end_1", "input"));
        buildGraph(List.of(start, end), edges, "start_1");

        AgentState state = createInitialState();
        state.getInputs().put("message", "你好");
        state.getInputs().put("userId", 123);

        executor.execute(graphDefinition, state);

        assertEquals("你好", state.getProcessedInput().get("message"));
        assertEquals(123, state.getProcessedInput().get("userId"));
    }

    @Test
    @DisplayName("未知节点类型应被跳过")
    void testUnknownNodeType_Skipped() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode unknown = createNode("unknown_1", "unknown_type", "未知节点");
        unknown.getInputPorts().add("input");
        unknown.getOutputPorts().add("output");

        GraphNode end = createNode("end_1", "end", "结束");
        end.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "unknown_1", "input"),
                createEdge("unknown_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, unknown, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("skipped", unknown.getStatus());
        assertEquals("completed", end.getStatus());
    }

    @Test
    @DisplayName("执行结果应包含正确的状态信息")
    void testExecutionResult_ContainsStateInfo() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode var = createNode("var_1", "variable", "设置变量");
        var.getConfig().put("name", "result");
        var.getConfig().put("value", "测试值");

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "var_1", "input"),
                createEdge("var_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, var, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertTrue(result.containsKey("outputs"));
        assertTrue(result.containsKey("variables"));
        assertTrue(result.containsKey("iterations"));
        assertTrue(result.containsKey("llmOutput"));
        assertTrue(result.containsKey("structuredOutput"));
        // 3 iterations: start -> var -> end
        assertEquals(3, result.get("iterations"));
    }

    @Test
    @DisplayName("节点执行顺序应正确记录")
    void testExecutionOrder() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode var = createNode("var_1", "variable", "变量");
        var.getConfig().put("name", "x");
        var.getConfig().put("value", "1");

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "var_1", "input"),
                createEdge("var_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, var, end), edges, "start_1");

        AgentState state = createInitialState();
        executor.execute(graphDefinition, state);

        assertEquals(1, start.getExecutionOrder());
        assertEquals(2, var.getExecutionOrder());
        assertEquals(3, end.getExecutionOrder());
    }

    @Test
    @DisplayName("Code节点 - JavaScript执行成功")
    void testCodeNode_Success() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode code = createNode("code_1", "code", "代码节点");
        code.getConfig().put("language", "javascript");
        code.getConfig().put("code", "2 + 3");
        code.getInputPorts().add("input");
        code.getOutputPorts().add("output");

        GraphNode end = createNode("end_1", "end", "结束");
        end.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "code_1", "input"),
                createEdge("code_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, code, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("5", code.getOutput());
    }

    @Test
    @DisplayName("Code节点 - 空代码应抛出异常")
    void testCodeNode_EmptyCode() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode code = createNode("code_1", "code", "代码节点");
        code.getConfig().put("language", "javascript");
        code.getConfig().put("code", "");

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "code_1", "input"),
                createEdge("code_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, code, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertEquals("failed", result.get("status"));
        assertEquals("failed", code.getStatus());
    }

    @Test
    @DisplayName("节点不存在应终止执行并报错")
    void testNonexistentNode() {
        GraphNode start = createNode("start_1", "start", "开始");
        // Edge points to a node that doesn't exist
        List<GraphEdge> edges = List.of(createEdge("start_1", "output", "ghost_node", "input"));
        buildGraph(List.of(start), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("failed", result.get("status"));
        assertTrue(((String) result.get("error")).contains("不存在"));
    }

    @Test
    @DisplayName("人工审批节点 - 当前自动批准并路由到approved分支")
    void testHumanApprovalNode() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode approval = createNode("approval_1", "human_approval", "人工审批");
        approval.getConfig().put("title", "请审批");
        approval.getConfig().put("approvalType", "approve_reject");
        approval.getInputPorts().add("input");
        approval.getOutputPorts().add("approved");
        approval.getOutputPorts().add("rejected");

        GraphNode endApproved = createNode("end_approved", "end", "已批准");
        endApproved.getInputPorts().add("input");

        GraphNode endRejected = createNode("end_rejected", "end", "已拒绝");
        endRejected.getInputPorts().add("input");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "approval_1", "input"),
                createEdge("approval_1", "approved", "end_approved", "input"),
                createEdge("approval_1", "rejected", "end_rejected", "input")
        );
        buildGraph(List.of(start, approval, endApproved, endRejected), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", endApproved.getStatus());
        assertEquals("pending", endRejected.getStatus());
        assertEquals("approved", state.getOutputs().get("approvalResult"));
    }

    @Test
    @DisplayName("子图节点 - 未配置agentId应抛出异常")
    void testSubgraphNode_MissingAgentId() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode subgraph = createNode("sub_1", "subgraph", "子图");
        // No agentId configured

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "sub_1", "input"),
                createEdge("sub_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, subgraph, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertEquals("failed", result.get("status"));
        assertEquals("failed", subgraph.getStatus());
    }

    @Test
    @DisplayName("子图节点 - 配置agentId应正常执行")
    void testSubgraphNode_WithAgentId() {
        GraphNode start = createNode("start_1", "start", "开始");
        GraphNode subgraph = createNode("sub_1", "subgraph", "子图");
        subgraph.getConfig().put("agentId", "agent_123");

        GraphNode end = createNode("end_1", "end", "结束");

        List<GraphEdge> edges = List.of(
                createEdge("start_1", "output", "sub_1", "input"),
                createEdge("sub_1", "output", "end_1", "input")
        );
        buildGraph(List.of(start, subgraph, end), edges, "start_1");

        AgentState state = createInitialState();
        Map<String, Object> result = executor.execute(graphDefinition, state);

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("completed", end.getStatus());
        assertTrue(subgraph.getOutput().contains("agent_123"));
    }
}
