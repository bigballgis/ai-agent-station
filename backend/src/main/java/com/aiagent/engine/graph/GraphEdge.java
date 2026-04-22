package com.aiagent.engine.graph;

import lombok.Data;

@Data
public class GraphEdge {
    private String id;
    private String sourceNodeId;
    private String sourcePort;      // 源端口名称
    private String targetNodeId;
    private String targetPort;      // 目标端口名称
    /**
     * 条件边标记（预留字段，用于条件分支路由）
     * 当前通过 sourcePort 区分 true/false 分支
     */
    // private String condition;  // 预留字段，当前未使用
}
