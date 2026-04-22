package com.aiagent.service.tool;

import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Function Tool -> LangChain4j ToolProvider 桥接器
 *
 * 将 FunctionToolRegistry 中注册的 Java 函数工具转换为 langchain4j ToolProvider 格式，
 * 使 langchain4j 的 AiServices 和 Tool Calling 循环可以直接调用 Function 工具。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionToolProviderBridge implements ToolProvider {

    private final FunctionToolRegistry functionToolRegistry;

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        List<ToolSpecification> specs = functionToolRegistry.toToolSpecifications();
        Map<ToolSpecification, ToolExecutor> toolMap = new LinkedHashMap<>();

        for (ToolSpecification spec : specs) {
            toolMap.put(spec, (toolExecutionRequest, memoryId) -> {
                String toolName = toolExecutionRequest.name();
                String arguments = toolExecutionRequest.arguments();
                log.debug("[FunctionToolBridge] 执行 Function 工具: {}({})", toolName, arguments);
                return functionToolRegistry.executeTool(toolName, arguments);
            });
        }

        log.debug("[FunctionToolBridge] 提供 {} 个 Function 工具", toolMap.size());
        return ToolProviderResult.builder().addAll(toolMap).build();
    }
}
