package com.aiagent.engine.graph;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图节点定义 — 对应前端画布中的一个节点
 */
@Data
public class GraphNode {
    private String id;
    private String type;       // llm, condition, tool, memory, variable, retriever, exception, http, code, start, end
    private String label;
    private Map<String, Object> config = new HashMap<>();
    private List<String> outputPorts = new ArrayList<>();  // 输出端口名称列表
    private Map<String, String> portMappings = new HashMap<>();  // 端口名 -> 目标节点ID
    private int maxRetries = 1;
    private int timeoutSeconds = 30;

    // 运行时状态
    private transient String status = "pending";  // pending, running, completed, failed, skipped
    private transient String output;              // 节点输出结果
    private transient String error;               // 错误信息
    private transient int executionOrder;         // 执行顺序
}
