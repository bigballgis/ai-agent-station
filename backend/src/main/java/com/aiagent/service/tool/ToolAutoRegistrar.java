package com.aiagent.service.tool;

import com.aiagent.service.tool.builtin.BuiltInTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 工具自动注册配置
 * 
 * 在 Spring 容器初始化完成后，自动扫描并注册所有带 @Tool 注解的工具类。
 * 
 * 注册流程:
 * 1. 扫描 BuiltInTools（内置工具集）
 * 2. 扫描所有标记了 @Component 且包含 @Tool 注解方法的 Bean
 * 3. 输出注册统计
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolAutoRegistrar {

    private final FunctionToolRegistry functionToolRegistry;
    private final BuiltInTools builtInTools;

    @EventListener(ApplicationReadyEvent.class)
    public void registerTools() {
        log.info("[ToolAutoRegistrar] 开始自动注册 Function Calling 工具...");

        // 1. 注册内置工具
        functionToolRegistry.registerAnnotatedMethods(builtInTools);

        // 2. 输出注册统计
        int totalTools = functionToolRegistry.size();
        log.info("[ToolAutoRegistrar] Function Calling 工具注册完成: 共 {} 个工具", totalTools);
        for (String group : functionToolRegistry.getGroups()) {
            log.info("[ToolAutoRegistrar]   分组 [{}]: {} 个工具 - {}",
                    group,
                    functionToolRegistry.getToolNamesByGroup(group).size(),
                    functionToolRegistry.getToolNamesByGroup(group));
        }
    }
}
