package com.aiagent.service.llm;

import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import com.aiagent.mcp.McpToolGateway;
import com.aiagent.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool -> LangChain4j ToolProvider 桥接器
 *
 * 将 MCP 工具网关暴露的工具转换为 langchain4j ToolProvider 格式，
 * 使 langchain4j 的 AiServices 和 Tool Calling 循环可以直接调用 MCP 工具。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolProviderBridge implements ToolProvider {

    private final McpToolGateway mcpToolGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 缓存的工具规格列表
     */
    private volatile List<ToolSpecification> cachedToolSpecs;
    private volatile long lastCacheTime = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1分钟缓存

    /**
     * 提供所有已注册 MCP 工具的规格定义
     */
    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        List<ToolSpecification> specs = loadToolSpecs();
        Map<ToolSpecification, ToolExecutor> toolMap = new LinkedHashMap<>();

        for (ToolSpecification spec : specs) {
            toolMap.put(spec, (toolExecutionRequest, memoryId) -> {
                String toolName = toolExecutionRequest.name();
                String arguments = toolExecutionRequest.arguments();

                log.info("[MCP Bridge] 执行 MCP 工具: {}({})", toolName, arguments);

                try {
                    Map<String, Object> parameters = objectMapper.readValue(arguments, Map.class);
                    Long toolId = resolveToolId(toolName);
                    if (toolId == null) {
                        return "{\"error\": \"工具不存在: " + toolName + "\"}";
                    }

                    Object result = mcpToolGateway.invokeTool(toolId, parameters, null, null);
                    String resultJson = objectMapper.writeValueAsString(result);
                    log.debug("[MCP Bridge] 工具执行结果: {}", resultJson);
                    return resultJson;
                } catch (Exception e) {
                    log.error("[MCP Bridge] 工具执行失败: {} - {}", toolName, e.getMessage());
                    return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
                }
            });
        }

        log.debug("[MCP Bridge] 提供 {} 个 MCP 工具", toolMap.size());
        return ToolProviderResult.builder().addAll(toolMap).build();
    }

    /**
     * 加载工具规格列表（带缓存）
     */
    private List<ToolSpecification> loadToolSpecs() {
        long now = System.currentTimeMillis();
        if (cachedToolSpecs != null && (now - lastCacheTime) < CACHE_TTL_MS) {
            return cachedToolSpecs;
        }

        try {
            List<ToolSpecification> specs = new ArrayList<>();
            List<Map<String, Object>> tools = mcpToolGateway.getAvailableTools();

            if (tools != null) {
                for (Map<String, Object> tool : tools) {
                    ToolSpecification spec = convertToToolSpec(tool);
                    if (spec != null) {
                        specs.add(spec);
                    }
                }
            }

            cachedToolSpecs = specs;
            lastCacheTime = now;
            log.debug("[MCP Bridge] 已转换 {} 个 MCP 工具为 langchain4j ToolSpecification", specs.size());
            return specs;
        } catch (ServiceUnavailableException e) {
            log.error("[MCP Bridge] MCP 服务不可用，无法获取工具列表: {}", e.getMessage());
            return cachedToolSpecs != null ? cachedToolSpecs : List.of();
        } catch (Exception e) {
            log.error("[MCP Bridge] 获取 MCP 工具列表失败: {}", e.getMessage());
            return cachedToolSpecs != null ? cachedToolSpecs : List.of();
        }
    }

    /**
     * 将 MCP 工具定义转换为 langchain4j ToolSpecification
     */
    private ToolSpecification convertToToolSpec(Map<String, Object> mcpTool) {
        try {
            String name = String.valueOf(mcpTool.getOrDefault("name", mcpTool.get("toolName")));
            String description = String.valueOf(mcpTool.getOrDefault("description", ""));

            ToolSpecification.Builder builder = ToolSpecification.builder()
                    .name(name)
                    .description(description);

            // 解析输入参数 schema
            Object inputSchema = mcpTool.get("inputSchema");
            if (inputSchema instanceof Map) {
                // MCP 工具的 inputSchema 是 Object 类型，需要强制转换为 Map
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = (Map<String, Object>) inputSchema;
                JsonObjectSchema jsonObjectSchema = convertSchemaToJsonObjectSchema(schema);
                if (jsonObjectSchema != null) {
                    builder.parameters(jsonObjectSchema);
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("[MCP Bridge] 工具规格转换失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 Map 形式的 JSON Schema 转换为 JsonObjectSchema
     */
    // 整个方法涉及 JSON Schema 的 Map/Object 递归转换，编译器无法验证泛型类型
    @SuppressWarnings("unchecked")
    private JsonObjectSchema convertSchemaToJsonObjectSchema(Map<String, Object> schema) {
        try {
            JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();

            if (schema.containsKey("description")) {
                schemaBuilder.description(String.valueOf(schema.get("description")));
            }

            if (schema.containsKey("properties")) {
                Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
                Map<String, JsonSchemaElement> propMap = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    propMap.put(entry.getKey(), convertJsonSchemaElement(entry.getValue()));
                }
                schemaBuilder.properties(propMap);
            }

            if (schema.containsKey("required")) {
                List<String> required = (List<String>) schema.get("required");
                schemaBuilder.required(required);
            }

            return schemaBuilder.build();
        } catch (Exception e) {
            log.warn("[MCP Bridge] Schema 转换失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 JSON Schema 元素转换为 JsonSchemaElement
     */
    // JSON Schema 元素是 Object 类型，递归转换时需要强制转换为 Map/List 等具体类型
    @SuppressWarnings("unchecked")
    private JsonSchemaElement convertJsonSchemaElement(Object element) {
        if (element instanceof String) {
            return mapSimpleType((String) element);
        }
        if (element instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) element;
            String type = String.valueOf(map.getOrDefault("type", "string"));
            if ("object".equals(type)) {
                return convertSchemaToJsonObjectSchema(map);
            }
            if ("array".equals(type)) {
                return JsonArraySchema.builder().build();
            }
            return mapSimpleType(type);
        }
        return JsonStringSchema.builder().build();
    }

    private JsonSchemaElement mapSimpleType(String type) {
        switch (type) {
            case "integer": return JsonIntegerSchema.builder().build();
            case "number": return JsonNumberSchema.builder().build();
            case "boolean": return JsonBooleanSchema.builder().build();
            default: return JsonStringSchema.builder().build();
        }
    }

    /**
     * 从工具名解析工具 ID
     */
    private Long resolveToolId(String toolName) {
        try {
            List<Map<String, Object>> tools = mcpToolGateway.getAvailableTools();
            if (tools != null) {
                for (Map<String, Object> tool : tools) {
                    String name = String.valueOf(tool.getOrDefault("name", tool.get("toolName")));
                    if (toolName.equals(name)) {
                        Object id = tool.get("id");
                        if (id instanceof Number) {
                            return ((Number) id).longValue();
                        }
                        return Long.parseLong(String.valueOf(id));
                    }
                }
            }
        } catch (Exception e) {
            log.error("[MCP Bridge] 解析工具 ID 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 强制刷新工具缓存
     */
    public void refreshToolCache() {
        cachedToolSpecs = null;
        lastCacheTime = 0;
        log.info("[MCP Bridge] 工具缓存已刷新");
    }
}
