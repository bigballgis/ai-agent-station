package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.Result;
import com.aiagent.service.tool.CompositeToolProvider;
import com.aiagent.service.tool.FunctionToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 工具管理 Controller
 *
 * 提供工具查询 API，展示所有可用工具（MCP + Function Calling）
 */
@Slf4j
@RestController
@RequestMapping("/v1/tools")
@RequiredArgsConstructor
@Tag(name = "工具管理", description = "工具管理接口")
public class ToolController {

    private final CompositeToolProvider compositeToolProvider;
    private final FunctionToolRegistry functionToolRegistry;

    /**
     * 获取所有可用工具列表
     * GET /api/v1/tools
     */
    @RequiresPermission("tool:view")
    @GetMapping
    @Operation(summary = "获取所有可用工具列表")
    public Result<Map<String, Object>> listTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // Function Calling 工具
        for (FunctionToolRegistry.FunctionToolDefinition def : functionToolRegistry.getAllTools()) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("name", def.getName());
            tool.put("description", def.getDescription());
            tool.put("group", def.getGroup());
            tool.put("source", "function");
            tool.put("parameterCount", def.parameters.size());
            tools.add(tool);
        }

        // MCP 工具数量（从 CompositeToolProvider 获取）
        int mcpCount = compositeToolProvider.getMcpToolCount();
        int functionCount = compositeToolProvider.getFunctionToolCount();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", tools.size() + mcpCount);
        result.put("functionTools", functionCount);
        result.put("mcpTools", mcpCount);
        result.put("tools", tools);

        return Result.success(result);
    }

    /**
     * 获取工具统计信息
     * GET /api/v1/tools/stats
     */
    @RequiresPermission("tool:read")
    @Operation(summary = "获取工具统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getToolStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTools", compositeToolProvider.getMcpToolCount() + compositeToolProvider.getFunctionToolCount());
        stats.put("mcpTools", compositeToolProvider.getMcpToolCount());
        stats.put("functionTools", compositeToolProvider.getFunctionToolCount());
        stats.put("functionGroups", functionToolRegistry.getGroups().size());
        stats.put("groups", functionToolRegistry.getGroups());

        Map<String, Integer> groupCounts = new LinkedHashMap<>();
        for (String group : functionToolRegistry.getGroups()) {
            groupCounts.put(group, functionToolRegistry.getToolNamesByGroup(group).size());
        }
        stats.put("groupCounts", groupCounts);

        return Result.success(stats);
    }

    /**
     * 查询工具来源
     * GET /api/v1/tools/{toolName}/source
     */
    @RequiresPermission("tool:read")
    @Operation(summary = "查询工具来源")
    @GetMapping("/{toolName}/source")
    public Result<Map<String, Object>> getToolSource(@PathVariable String toolName) {
        String source = compositeToolProvider.getToolSource(toolName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolName", toolName);
        result.put("source", source);
        result.put("description", source.equals("function") ? "系统内置 Java 函数" : "外部 MCP 服务器工具");
        return Result.success(result);
    }

    /**
     * 刷新工具缓存
     * POST /api/v1/tools/refresh
     */
    @Operation(summary = "刷新工具缓存")
    @RequiresPermission("tool:manage")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshTools() {
        compositeToolProvider.refreshCache();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "工具缓存已刷新");
        result.put("totalTools", compositeToolProvider.getMcpToolCount() + compositeToolProvider.getFunctionToolCount());
        return Result.success(result);
    }
}
