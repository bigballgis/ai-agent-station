package com.aiagent.service.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Function Calling 工具注册中心
 *
 * 核心能力:
 * 1. 注册 Java 方法为可调用工具（手动注册 + @Tool 注解扫描）
 * 2. 将 Java 方法签名自动转换为 langchain4j ToolSpecification
 * 3. 通过反射调用注册的 Java 方法执行工具
 * 4. 支持工具分组、描述、权限标记
 */
@Slf4j
@Component
public class FunctionToolRegistry {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 工具注册表: toolName -> FunctionToolDefinition
     */
    private final Map<String, FunctionToolDefinition> registry = new ConcurrentHashMap<>();

    /**
     * 工具分组: groupName -> Set<toolName>
     */
    private final Map<String, Set<String>> groups = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[FunctionToolRegistry] 工具注册中心初始化");
    }

    // ==================== 手动注册 ====================

    /**
     * 注册一个函数工具（无参数）
     */
    public void register(String name, String description, ToolExecutor executor) {
        register(name, description, "utility", executor, Collections.emptyList());
    }

    /**
     * 注册一个函数工具（带参数定义）
     */
    public void register(String name, String description, String group,
                         ToolExecutor executor, List<ToolParameter> parameters) {
        FunctionToolDefinition def = new FunctionToolDefinition();
        def.name = name;
        def.description = description;
        def.group = group;
        def.executor = executor;
        def.parameters = parameters;
        def.source = "MANUAL";

        registry.put(name, def);
        groups.computeIfAbsent(group, g -> ConcurrentHashMap.newKeySet()).add(name);
        log.info("[FunctionToolRegistry] 注册工具: {} (group={}, params={})", name, group, parameters.size());
    }

    /**
     * 注册一个函数工具（使用 Java Method 反射）
     */
    public void register(String name, String description, String group,
                         Object instance, Method method) {
        List<ToolParameter> parameters = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            ToolParameter tp = new ToolParameter();
            tp.name = param.getName();
            tp.type = mapJavaTypeToJsonSchema(param.getType());
            tp.description = "";
            tp.required = true;
            parameters.add(tp);
        }

        register(name, description, group,
                (args) -> {
                    try {
                        JsonNode argsNode = objectMapper.readTree(args);
                        Object[] argValues = new Object[parameters.size()];
                        for (int i = 0; i < parameters.size(); i++) {
                            JsonNode val = argsNode.path(parameters.get(i).name);
                            argValues[i] = objectMapper.treeToValue(val, method.getParameterTypes()[i]);
                        }
                        Object result = method.invoke(instance, argValues);
                        return result != null ? objectMapper.writeValueAsString(result) : "{}";
                    } catch (Exception e) {
                        return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
                    }
                },
                parameters);
    }

    // ==================== @Tool 注解扫描注册 ====================

    /**
     * 扫描对象中所有带 @Tool 注解的方法并注册
     */
    public void registerAnnotatedMethods(Object toolInstance) {
        Class<?> clazz = toolInstance.getClass();
        String defaultGroup = clazz.getSimpleName();

        for (Method method : clazz.getDeclaredMethods()) {
            dev.langchain4j.agent.tool.Tool toolAnnotation = method.getAnnotation(dev.langchain4j.agent.tool.Tool.class);
            if (toolAnnotation == null) continue;

            String name = toolAnnotation.name().isEmpty() ? method.getName() : toolAnnotation.name();
            String[] values = toolAnnotation.value();
            String description = (values != null && values.length > 0 && !values[0].isEmpty())
                    ? values[0] : name;

            // 解析参数
            List<ToolParameter> parameters = new ArrayList<>();
            for (Parameter param : method.getParameters()) {
                ToolParameter tp = new ToolParameter();
                tp.name = param.getName();
                tp.type = mapJavaTypeToJsonSchema(param.getType());
                tp.description = "";
                tp.required = true;
                parameters.add(tp);
            }

            FunctionToolDefinition def = new FunctionToolDefinition();
            def.name = name;
            def.description = description;
            def.group = defaultGroup;
            def.parameters = parameters;
            def.source = "ANNOTATION";
            def.instance = toolInstance;
            def.method = method;

            // 创建执行器
            final Method targetMethod = method;
            final Object targetInstance = toolInstance;
            final List<ToolParameter> targetParams = parameters;

            def.executor = (args) -> {
                try {
                    JsonNode argsNode = objectMapper.readTree(args);
                    Object[] argValues = new Object[targetParams.size()];
                    Class<?>[] paramTypes = targetMethod.getParameterTypes();
                    for (int i = 0; i < targetParams.size(); i++) {
                        JsonNode val = argsNode.path(targetParams.get(i).name);
                        if (val.isMissingNode() || val.isNull()) {
                            argValues[i] = getDefaultForType(paramTypes[i]);
                        } else {
                            argValues[i] = objectMapper.treeToValue(val, paramTypes[i]);
                        }
                    }
                    targetMethod.setAccessible(true);
                    Object result = targetMethod.invoke(targetInstance, argValues);
                    return result != null ? objectMapper.writeValueAsString(result) : "{}";
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    return "{\"error\": \"" + (cause != null ? cause.getMessage() : e.getMessage()).replace("\"", "'") + "\"}";
                } catch (Exception e) {
                    return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
                }
            };

            registry.put(name, def);
            groups.computeIfAbsent(defaultGroup, g -> ConcurrentHashMap.newKeySet()).add(name);
            log.info("[FunctionToolRegistry] @Tool 扫描注册: {}.{} -> {} (params={})",
                    clazz.getSimpleName(), method.getName(), name, parameters.size());
        }
    }

    // ==================== 工具查询 ====================

    /**
     * 获取所有已注册工具名称
     */
    public Set<String> getToolNames() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /**
     * 获取指定分组的工具名称
     */
    public Set<String> getToolNamesByGroup(String group) {
        return groups.getOrDefault(group, Collections.emptySet());
    }

    /**
     * 获取所有分组名称
     */
    public Set<String> getGroups() {
        return Collections.unmodifiableSet(groups.keySet());
    }

    /**
     * 检查工具是否已注册
     */
    public boolean hasTool(String toolName) {
        return registry.containsKey(toolName);
    }

    /**
     * 获取工具定义
     */
    public Optional<FunctionToolDefinition> getTool(String toolName) {
        return Optional.ofNullable(registry.get(toolName));
    }

    /**
     * 获取所有工具定义
     */
    public Collection<FunctionToolDefinition> getAllTools() {
        return Collections.unmodifiableCollection(registry.values());
    }

    // ==================== 工具执行 ====================

    /**
     * 执行指定工具
     */
    public String executeTool(String toolName, String arguments) {
        FunctionToolDefinition def = registry.get(toolName);
        if (def == null) {
            return "{\"error\": \"Function tool not found: " + toolName + "\"}";
        }
        try {
            log.debug("[FunctionToolRegistry] 执行工具: {}({})", toolName, arguments);
            long start = System.currentTimeMillis();
            String result = def.executor.execute(arguments);
            long elapsed = System.currentTimeMillis() - start;
            log.debug("[FunctionToolRegistry] 工具执行完成: {} ({}ms)", toolName, elapsed);
            return result;
        } catch (Exception e) {
            log.error("[FunctionToolRegistry] 工具执行失败: {} - {}", toolName, e.getMessage());
            return "{\"error\": \"Tool execution failed: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    /**
     * 获取工具数量
     */
    public int size() {
        return registry.size();
    }

    // ==================== 转换为 langchain4j ToolSpecification ====================

    /**
     * 将所有注册工具转换为 langchain4j ToolSpecification 列表
     */
    public List<ToolSpecification> toToolSpecifications() {
        List<ToolSpecification> specs = new ArrayList<>();
        for (FunctionToolDefinition def : registry.values()) {
            specs.add(toToolSpecification(def));
        }
        return specs;
    }

    /**
     * 将单个工具定义转换为 langchain4j ToolSpecification
     */
    public ToolSpecification toToolSpecification(FunctionToolDefinition def) {
        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(def.name)
                .description(def.description);

        if (!def.parameters.isEmpty()) {
            try {
                JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
                Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();
                List<String> required = new ArrayList<>();

                for (ToolParameter param : def.parameters) {
                    JsonSchemaElement paramSchema = mapTypeToJsonSchemaElement(param);
                    properties.put(param.name, paramSchema);
                    if (param.required) {
                        required.add(param.name);
                    }
                }

                schemaBuilder.properties(properties);
                if (!required.isEmpty()) {
                    schemaBuilder.required(required);
                }

                builder.parameters(schemaBuilder.build());
            } catch (Exception e) {
                log.warn("[FunctionToolRegistry] 工具参数序列化失败: {} - {}", def.name, e.getMessage());
            }
        }

        return builder.build();
    }

    /**
     * 将 ToolParameter 映射为 JsonSchemaElement
     */
    private JsonSchemaElement mapTypeToJsonSchemaElement(ToolParameter param) {
        switch (param.type) {
            case "integer":
                return JsonIntegerSchema.builder().build();
            case "number":
                return JsonNumberSchema.builder().build();
            case "boolean":
                return JsonBooleanSchema.builder().build();
            case "array":
                return JsonArraySchema.builder().build();
            case "object":
                return JsonObjectSchema.builder().build();
            default:
                return JsonStringSchema.builder().build();
        }
    }

    // ==================== 工具卸载 ====================

    /**
     * 卸载指定工具
     */
    public void unregister(String toolName) {
        FunctionToolDefinition def = registry.remove(toolName);
        if (def != null) {
            Set<String> groupTools = groups.get(def.group);
            if (groupTools != null) {
                groupTools.remove(toolName);
                if (groupTools.isEmpty()) {
                    groups.remove(def.group);
                }
            }
            log.info("[FunctionToolRegistry] 卸载工具: {}", toolName);
        }
    }

    /**
     * 卸载指定分组的所有工具
     */
    public void unregisterGroup(String group) {
        Set<String> toolNames = groups.remove(group);
        if (toolNames != null) {
            toolNames.forEach(registry::remove);
            log.info("[FunctionToolRegistry] 卸载分组: {} ({} 个工具)", group, toolNames.size());
        }
    }

    // ==================== 辅助方法 ====================

    private String mapJavaTypeToJsonSchema(Class<?> type) {
        if (type == String.class) return "string";
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class) return "integer";
        if (type == Double.class || type == double.class || type == Float.class || type == float.class) return "number";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (type.isArray() || Collection.class.isAssignableFrom(type)) return "array";
        if (Map.class.isAssignableFrom(type)) return "object";
        return "string";
    }

    private Object getDefaultForType(Class<?> type) {
        if (type == String.class) return "";
        if (type == Integer.class || type == int.class) return 0;
        if (type == Long.class || type == long.class) return 0L;
        if (type == Double.class || type == double.class) return 0.0;
        if (type == Float.class || type == float.class) return 0.0f;
        if (type == Boolean.class || type == boolean.class) return false;
        return null;
    }

    // ==================== 内部数据结构 ====================

    /**
     * 函数工具执行器
     */
    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String arguments) throws Exception;
    }

    /**
     * 函数工具定义
     */
    public static class FunctionToolDefinition {
        public String name;
        public String description;
        public String group;
        public String source;  // MANUAL / ANNOTATION
        public List<ToolParameter> parameters;
        public ToolExecutor executor;
        public Object instance;
        public Method method;

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getGroup() { return group; }
    }

    /**
     * 工具参数定义
     */
    public static class ToolParameter {
        public String name;
        public String type;       // JSON Schema type: string, integer, number, boolean, array, object
        public String description;
        public boolean required = true;
        public List<String> enumValues;
    }
}
