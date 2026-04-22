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

                    // 设置默认输出端口
                    node.getOutputPorts().add("output");

                    // 条件节点添加 true/false 端口
                    if ("condition".equals(node.getType())) {
                        node.getOutputPorts().clear();
                        node.getOutputPorts().add("true");
                        node.getOutputPorts().add("false");
                    }

                    graph.getNodes().put(node.getId(), node);
                }
            }

            // 解析连接（前端叫 connections，后端叫 edges）
            List<Map<String, Object>> connections = (List<Map<String, Object>>) graphMap.get("connections");
            if (connections != null) {
                for (Map<String, Object> connData : connections) {
                    GraphEdge edge = new GraphEdge();
                    edge.setId(UUID.randomUUID().toString());
                    edge.setSourceNodeId((String) connData.get("sourceId"));
                    edge.setSourcePort((String) connData.getOrDefault("sourcePort", "output"));
                    edge.setTargetNodeId((String) connData.get("targetId"));
                    edge.setTargetPort((String) connData.getOrDefault("targetPort", "input"));
                    graph.getEdges().add(edge);
                }
            }

            // 如果没有 start 节点，自动创建
            if (!graph.getNodes().containsKey("start")) {
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
}
