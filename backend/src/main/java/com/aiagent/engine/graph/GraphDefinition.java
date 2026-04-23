package com.aiagent.engine.graph;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图定义 — 从 Agent.config 中解析的完整图结构
 */
@Data
public class GraphDefinition {
    private String entryNodeId;
    private Map<String, GraphNode> nodes = new HashMap<>();
    private List<GraphEdge> edges = new ArrayList<>();

    public GraphNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<GraphEdge> getOutgoingEdges(String nodeId) {
        List<GraphEdge> result = new ArrayList<>();
        for (GraphEdge edge : edges) {
            if (edge.getSourceNodeId().equals(nodeId)) {
                result.add(edge);
            }
        }
        return result;
    }

    public List<GraphEdge> getIncomingEdges(String nodeId) {
        List<GraphEdge> result = new ArrayList<>();
        for (GraphEdge edge : edges) {
            if (edge.getTargetNodeId().equals(nodeId)) {
                result.add(edge);
            }
        }
        return result;
    }

    public GraphEdge getEdgeBySourcePort(String nodeId, String portName) {
        for (GraphEdge edge : edges) {
            if (edge.getSourceNodeId().equals(nodeId) && portName.equals(edge.getSourcePort())) {
                return edge;
            }
        }
        return null;
    }

    /**
     * 验证图结构合法性
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (entryNodeId == null || !nodes.containsKey(entryNodeId)) {
            errors.add("入口节点未定义或不存在");
        }
        // 检查孤立节点（无入边也无出边，且不是入口节点）
        for (GraphNode node : nodes.values()) {
            if (!node.getId().equals(entryNodeId)) {
                boolean hasIncoming = edges.stream().anyMatch(e -> e.getTargetNodeId().equals(node.getId()));
                boolean hasOutgoing = edges.stream().anyMatch(e -> e.getSourceNodeId().equals(node.getId()));
                if (!hasIncoming && !hasOutgoing) {
                    errors.add("节点 " + node.getId() + " (" + node.getLabel() + ") 是孤立的");
                }
            }
        }
        // 检测循环依赖（DFS）
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        for (GraphNode node : nodes.values()) {
            if (hasCycle(node.getId(), visited, recursionStack)) {
                errors.add("图中存在循环依赖，包含节点: " + node.getId());
                break;
            }
        }
        return errors;
    }

    /**
     * 将图定义导出为前端期望的 JSON 友好格式
     */
    public Map<String, Object> toJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("entryNodeId", entryNodeId);

        List<Map<String, Object>> nodeJsons = new ArrayList<>();
        for (GraphNode node : nodes.values()) {
            Map<String, Object> nodeJson = new HashMap<>();
            nodeJson.put("id", node.getId());
            nodeJson.put("type", node.getType());
            nodeJson.put("label", node.getLabel());
            nodeJson.put("config", node.getConfig());
            nodeJsons.add(nodeJson);
        }
        json.put("nodes", nodeJsons);

        List<Map<String, Object>> edgeJsons = new ArrayList<>();
        for (GraphEdge edge : edges) {
            Map<String, Object> edgeJson = new HashMap<>();
            edgeJson.put("sourceId", edge.getSourceNodeId());
            edgeJson.put("sourcePort", edge.getSourcePort());
            edgeJson.put("targetId", edge.getTargetNodeId());
            edgeJson.put("targetPort", edge.getTargetPort());
            edgeJsons.add(edgeJson);
        }
        json.put("connections", edgeJsons);

        return json;
    }

    private boolean hasCycle(String nodeId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) return true;
        if (visited.contains(nodeId)) return false;
        visited.add(nodeId);
        recursionStack.add(nodeId);
        for (GraphEdge edge : edges) {
            if (edge.getSourceNodeId().equals(nodeId)) {
                if (hasCycle(edge.getTargetNodeId(), visited, recursionStack)) return true;
            }
        }
        recursionStack.remove(nodeId);
        return false;
    }
}
