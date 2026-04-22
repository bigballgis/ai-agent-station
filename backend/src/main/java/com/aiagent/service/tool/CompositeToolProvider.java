package com.aiagent.service.tool;

import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 复合工具提供者 -- 统一 MCP 工具 + Function 工具
 *
 * 将 McpToolProviderBridge（外部 MCP 工具）和 FunctionToolProviderBridge（内部 Java 函数工具）
 * 合并为一个统一的 ToolProvider，供 GraphExecutor 和 AiServices 使用。
 *
 * 工具名称冲突处理:
 * - Function 工具优先级高于 MCP 工具
 * - 如果两者有同名工具，Function 工具会覆盖 MCP 工具
 * - 通过 getToolSource() 可查询工具来源
 */
@Slf4j
@Component
public class CompositeToolProvider implements ToolProvider {

    private final FunctionToolProviderBridge functionToolBridge;
    private final com.aiagent.service.llm.McpToolProviderBridge mcpToolBridge;

    /**
     * 工具来源缓存: toolName -> "mcp" / "function"
     */
    private volatile Map<String, String> toolSourceMap;
    private volatile long cacheTime = 0;
    private static final long CACHE_TTL_MS = 30_000;

    public CompositeToolProvider(
            FunctionToolProviderBridge functionToolBridge,
            com.aiagent.service.llm.McpToolProviderBridge mcpToolBridge) {
        this.functionToolBridge = functionToolBridge;
        this.mcpToolBridge = mcpToolBridge;
        log.info("[CompositeToolProvider] 初始化: MCP + Function 双通道工具提供者");
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        long now = System.currentTimeMillis();
        if (toolSourceMap != null && (now - cacheTime) < CACHE_TTL_MS) {
            // 缓存有效，重新构建 ToolProviderResult
            return buildToolProviderResult();
        }

        // 合并两个工具源
        Map<String, String> sourceMap = new java.util.HashMap<>();

        // 先添加 MCP 工具
        try {
            ToolProviderResult mcpResult = mcpToolBridge.provideTools(request);
            for (ToolSpecification spec : mcpResult.tools().keySet()) {
                sourceMap.put(spec.name(), "mcp");
            }
        } catch (Exception e) {
            log.warn("[CompositeToolProvider] MCP 工具加载失败: {}", e.getMessage());
        }

        // 再添加 Function 工具（同名覆盖 MCP）
        try {
            ToolProviderResult funcResult = functionToolBridge.provideTools(request);
            for (ToolSpecification spec : funcResult.tools().keySet()) {
                sourceMap.put(spec.name(), "function");
            }
        } catch (Exception e) {
            log.warn("[CompositeToolProvider] Function 工具加载失败: {}", e.getMessage());
        }

        toolSourceMap = sourceMap;
        cacheTime = now;

        log.info("[CompositeToolProvider] 工具加载完成: MCP={}, Function={}, 总计={}",
                sourceMap.values().stream().filter(s -> s.equals("mcp")).count(),
                sourceMap.values().stream().filter(s -> s.equals("function")).count(),
                sourceMap.size());

        return buildToolProviderResult();
    }

    /**
     * 构建 ToolProviderResult，合并 MCP 和 Function 工具
     * Function 工具同名覆盖 MCP 工具
     */
    private ToolProviderResult buildToolProviderResult() {
        ToolProviderRequest dummyRequest = new ToolProviderRequest(null, null);
        Map<ToolSpecification, ToolExecutor> mergedTools = new LinkedHashMap<>();

        // 先添加 MCP 工具
        try {
            ToolProviderResult mcpResult = mcpToolBridge.provideTools(dummyRequest);
            mergedTools.putAll(mcpResult.tools());
        } catch (Exception e) {
            log.warn("[CompositeToolProvider] MCP 工具合并失败: {}", e.getMessage());
        }

        // 再添加 Function 工具（同名覆盖 MCP）
        try {
            ToolProviderResult funcResult = functionToolBridge.provideTools(dummyRequest);
            mergedTools.putAll(funcResult.tools());
        } catch (Exception e) {
            log.warn("[CompositeToolProvider] Function 工具合并失败: {}", e.getMessage());
        }

        return ToolProviderResult.builder().addAll(mergedTools).build();
    }

    /**
     * 获取所有工具规格列表（兼容旧代码）
     */
    public List<ToolSpecification> getToolSpecifications() {
        ToolProviderResult result = provideTools(new ToolProviderRequest(null, null));
        return new ArrayList<>(result.tools().keySet());
    }

    /**
     * 执行工具（兼容旧代码）
     */
    public String executeTool(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        String toolName = toolExecutionRequest.name();
        String source = getToolSource(toolName);

        log.info("[CompositeToolProvider] 执行工具: {} (source={})", toolName, source);

        if ("function".equals(source)) {
            ToolProviderResult funcResult = functionToolBridge.provideTools(new ToolProviderRequest(memoryId, null));
            for (Map.Entry<ToolSpecification, ToolExecutor> entry : funcResult.tools().entrySet()) {
                if (entry.getKey().name().equals(toolName)) {
                    return entry.getValue().execute(toolExecutionRequest, memoryId);
                }
            }
        } else {
            ToolProviderResult mcpResult = mcpToolBridge.provideTools(new ToolProviderRequest(memoryId, null));
            for (Map.Entry<ToolSpecification, ToolExecutor> entry : mcpResult.tools().entrySet()) {
                if (entry.getKey().name().equals(toolName)) {
                    return entry.getValue().execute(toolExecutionRequest, memoryId);
                }
            }
        }

        return "{\"error\": \"Tool not found: " + toolName + "\"}";
    }

    /**
     * 查询工具来源
     */
    public String getToolSource(String toolName) {
        if (toolSourceMap == null) {
            provideTools(new ToolProviderRequest(null, null)); // 触发加载
        }
        return toolSourceMap != null ? toolSourceMap.getOrDefault(toolName, "mcp") : "mcp";
    }

    /**
     * 获取 MCP 工具数量
     */
    public int getMcpToolCount() {
        provideTools(new ToolProviderRequest(null, null));
        return (int) toolSourceMap.values().stream().filter(s -> s.equals("mcp")).count();
    }

    /**
     * 获取 Function 工具数量
     */
    public int getFunctionToolCount() {
        provideTools(new ToolProviderRequest(null, null));
        return (int) toolSourceMap.values().stream().filter(s -> s.equals("function")).count();
    }

    /**
     * 强制刷新工具缓存
     */
    public void refreshCache() {
        toolSourceMap = null;
        cacheTime = 0;
        log.info("[CompositeToolProvider] 工具缓存已刷新");
    }
}
