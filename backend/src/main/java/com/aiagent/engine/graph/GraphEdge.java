package com.aiagent.engine.graph;

import lombok.Data;

@Data
public class GraphEdge {
    private String id;
    private String sourceNodeId;
    private String sourcePort;      // 源端口名称
    private String targetNodeId;
    private String targetPort;      // 目标端口名称
}
