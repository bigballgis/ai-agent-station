package com.aiagent.engine.graph;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 图定义解析器 — 将前端画布 JSON 转换为 GraphDefinition
 */
@Slf4j
@Component
public class GraphParser {

    /**
     * 从 Agent config 中的图定义 JSON 解析为 GraphDefinition
     *
     * 前端画布数据格式:
     * {
     *   "nodes": [{ "id": "node1", "type": "llm", "label": "LLM调用", "config": {...}, "position": {...} }],
     *   "connections": [{ "sourceId": "node1", "sourcePort": "output", "targetId": "node2", "targetPort": "input" }],
     *   "entryNodeId": "node1"
     * }
     */
    @SuppressWarnings("unchecked")
    public GraphDefinition parse(Map<String, Object> agentConfig) {
        GraphDefinition graph = new GraphDefinition();

        Object graphData = agentConfig.get("graph");
        if (graphData == null) {
            // 如果没有图定义，创建默认的线性流程
            log.info("Agent 无图定义，使用默认线性流程");
            return createDefaultGraph(agentConfig);
        }

        try {
            Map<String, Object> graphMap = (Map<String, Object>) graphData;

            // 解析入口节点
            graph.setEntryNodeId((String) graphMap.getOrDefault("entryNodeId", "start"));

            // 解析节点
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) graphMap.get("nodes");
            if (nodes != null) {
                for (Map<String, Object> nodeData : nodes) {
                    GraphNode node = new GraphNode();
                    node.setId((String) nodeData.get("id"));
                    node.setType((String) nodeData.getOrDefault("type", "llm"));
                    node.setLabel((String) nodeData.getOrDefault("label", node.getType()));

                    if (nodeData.get("config") != null) {
                        Map<String, Object> config = (Map<String, Object>) nodeData.get("config");
                        node.setConfig(config);
                    }

                    // 处理节点位置 — 兼容嵌套 position:{x,y} 和顶层 x,y 两种格式
                    Object posObj = nodeData.get("position");
                    if (posObj instanceof Map) {
                        Map<String, Object> pos = (Map<String, Object>) posObj;
                        // 嵌套格式：position 字段包含 x, y — 后端无需特殊处理，仅记录日志
                        log.debug("节点 {} 位置（嵌套格式）: {}", node.getId(), pos);
                    } else {
                        // 旧格式：x, y 作为顶层字段 — 同样无需特殊处理
                        log.debug("节点 {} 位置（顶层格式）: x={}, y={}", node.getId(), nodeData.get("x"), nodeData.get("y"));
                    }

                    // 设置默认输出端口
                    node.getOutputPorts().add("output");

                    // 条件节点添加 true/false 端口
                    if ("condition".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("true");
                        node.getOutputPorts().add("false");
                    }

                    // 工具节点端口
                    if ("tool".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // 记忆节点端口
                    if ("memory".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // 变量节点端口
                    if ("variable".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // 检索节点端口
                    if ("retriever".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // 异常处理节点端口
                    if ("exception".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // HTTP 请求节点端口
                    if ("http".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // 代码节点端口
                    if ("code".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // 延时节点端口
                    if ("delay".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // 人工审批节点端口（approved/rejected 双输出）
                    if ("human_approval".equals(node.getType())) {
                        node.getInputPorts().clear();
                        node.getInputPorts().add("input");
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("approved");
                        node.getOutputPorts().add("rejected");
                    }

                    // 子图/Agent 调用节点端口
                    if ("subgraph".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getInputPorts().clear();
                        node.getInputPorts().add("input");
                        node.getOutputPorts().add("output");
                    }

                    // 多路分支节点端口
                    if ("switch".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getInputPorts().clear();
                        node.getInputPorts().add("input");
                        // 动态端口基于 cases 配置
                        Object casesObj = node.getConfig().get("cases");
                        if (casesObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, String>> cases = (List<Map<String, String>>) casesObj;
                            for (Map<String, String> caseDef : cases) {
                                String port = caseDef.getOrDefault("outputPort", "case_" + node.getOutputPorts().size());
                                node.getOutputPorts().add(port);
                            }
                        }
                        node.getOutputPorts().add("default");
                    }

                    // 并行执行节点端口
                    if ("parallel".equals(node.getType())) {
                        node.getInputPorts().clear();
                        node.getInputPorts().add("input");
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    // 合并节点端口
                    if ("merge".equals(node.getType())) {
                        node.getInputPorts().clear();
                        node.getInputPorts().add("input_1");
                        node.getInputPorts().add("input_2");
                        node.getInputPorts().add("input_3");
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("output");
                    }

                    graph.getNodes().put(node.getId(), node);
                }
            }

            // 解析连接（前端叫 connections，后端叫 edges）
            // 兼容旧格式 (fromNodeId/toNodeId) 和新格式 (sourceId/targetId)
            List<Map<String, Object>> connections = (List<Map<String, Object>>) graphMap.get("connections");
            if (connections != null) {
                for (Map<String, Object> connData : connections) {
                    GraphEdge edge = new GraphEdge();
                    edge.setId(UUID.randomUUID().toString());

                    // 解析连接 — 兼容旧格式 (fromNodeId/toNodeId) 和新格式 (sourceId/targetId)
                    String sourceId = (String) connData.get("sourceId");
                    if (sourceId == null) sourceId = (String) connData.get("fromNodeId"); // 向后兼容
                    String sourcePort = (String) connData.get("sourcePort");
                    if (sourcePort == null) sourcePort = (String) connData.get("fromPort"); // 向后兼容
                    String targetId = (String) connData.get("targetId");
                    if (targetId == null) targetId = (String) connData.get("toNodeId"); // 向后兼容
                    String targetPort = (String) connData.get("targetPort");
                    if (targetPort == null) targetPort = (String) connData.get("toPort"); // 向后兼容

                    // 规范化端口名称，处理前端不同格式
                    sourcePort = normalizePortName(sourcePort);
                    targetPort = normalizePortName(targetPort);

                    edge.setSourceNodeId(sourceId);
                    edge.setSourcePort(sourcePort != null ? sourcePort : "output");
                    edge.setTargetNodeId(targetId);
                    edge.setTargetPort(targetPort != null ? targetPort : "input");
                    graph.getEdges().add(edge);
                }
            }

            // 如果没有 start 节点，自动创建
            boolean hasStartNode = graph.getNodes().values().stream()
                .anyMatch(n -> "start".equals(n.getType()));
            if (!hasStartNode) {
                GraphNode startNode = new GraphNode();
                startNode.setId("start");
                startNode.setType("start");
                startNode.setLabel("开始");
                graph.getNodes().put("start", startNode);
                if (graph.getEntryNodeId() == null) {
                    graph.setEntryNodeId("start");
                }
            }

            log.info("图解析完成: {} 个节点, {} 条边, 入口: {}",
                graph.getNodes().size(), graph.getEdges().size(), graph.getEntryNodeId());

        } catch (Exception e) {
            log.error("图定义解析失败: {}", e.getMessage());
            return createDefaultGraph(agentConfig);
        }

        return graph;
    }

    /**
     * 创建默认线性图（兼容旧配置格式）
     */
    private GraphDefinition createDefaultGraph(Map<String, Object> agentConfig) {
        GraphDefinition graph = new GraphDefinition();

        // start -> llm -> end
        GraphNode startNode = new GraphNode();
        startNode.setId("start");
        startNode.setType("start");
        startNode.setLabel("开始");
        startNode.getOutputPorts().add("output");

        GraphNode llmNode = new GraphNode();
        llmNode.setId("llm-1");
        llmNode.setType("llm");
        llmNode.setLabel("LLM 调用");
        llmNode.setConfig(agentConfig);
        llmNode.getOutputPorts().add("output");

        GraphNode endNode = new GraphNode();
        endNode.setId("end");
        endNode.setType("end");
        endNode.setLabel("结束");

        graph.setEntryNodeId("start");
        graph.getNodes().put("start", startNode);
        graph.getNodes().put("llm-1", llmNode);
        graph.getNodes().put("end", endNode);

        graph.getEdges().add(createEdge("start", "output", "llm-1", "input"));
        graph.getEdges().add(createEdge("llm-1", "output", "end", "input"));

        return graph;
    }

    private GraphEdge createEdge(String sourceId, String sourcePort, String targetId, String targetPort) {
        GraphEdge edge = new GraphEdge();
        edge.setId(UUID.randomUUID().toString());
        edge.setSourceNodeId(sourceId);
        edge.setSourcePort(sourcePort);
        edge.setTargetNodeId(targetId);
        edge.setTargetPort(targetPort);
        return edge;
    }

    /**
     * 规范化端口名称，处理前端不同格式的端口命名
     * 例如: "output-true" -> "true", "output-false" -> "false", "output" -> "output"
     */
    private String normalizePortName(String portName) {
        if (portName == null) return "output";
        if ("output-true".equals(portName)) return "true";
        if ("output-false".equals(portName)) return "false";
        return portName;
    }
}
