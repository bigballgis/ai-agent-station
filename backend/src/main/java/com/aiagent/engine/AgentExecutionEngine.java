package com.aiagent.engine;

import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.dto.AgentInvokeRequest;
import com.aiagent.dto.AgentInvokeResponse;
import com.aiagent.engine.graph.AgentState;
import com.aiagent.engine.graph.GraphDefinition;
import com.aiagent.engine.graph.GraphExecutor;
import com.aiagent.engine.graph.GraphParser;
import com.aiagent.entity.Agent;
import com.aiagent.repository.AgentRepository;
import com.aiagent.service.ReflectionEvaluationService;
import com.aiagent.service.llm.LangChain4jService;
import com.aiagent.util.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AgentExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionEngine.class);
    private static final String ASYNC_TASK_KEY_PREFIX = "async:task:";
    private static final long ASYNC_TASK_TTL_HOURS = 1;

    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;
    private final ReflectionEvaluationService reflectionEvaluationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final LangChain4jService langChain4jService;
    private final GraphParser graphParser;
    private final GraphExecutor graphExecutor;
    private final AiAgentProperties aiAgentProperties;

    public AgentExecutionEngine(AgentRepository agentRepository, ObjectMapper objectMapper,
                                 ReflectionEvaluationService reflectionEvaluationService,
                                 StringRedisTemplate stringRedisTemplate,
                                 LangChain4jService langChain4jService,
                                 GraphParser graphParser,
                                 GraphExecutor graphExecutor,
                                 AiAgentProperties aiAgentProperties) {
        this.agentRepository = agentRepository;
        this.objectMapper = objectMapper;
        this.reflectionEvaluationService = reflectionEvaluationService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.langChain4jService = langChain4jService;
        this.graphParser = graphParser;
        this.graphExecutor = graphExecutor;
        this.aiAgentProperties = aiAgentProperties;
    }

    public AgentInvokeResponse invokeAgent(Long agentId, AgentInvokeRequest request, String requestId, Long tenantId) {
        long startTime = System.currentTimeMillis();
        AgentInvokeResponse response = new AgentInvokeResponse();
        response.setRequestId(requestId);

        try {
            // 查找Agent
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent not found"));

            // 检查Agent是否已发布
            if (agent.getStatus() != Agent.AgentStatus.PUBLISHED) {
                throw new RuntimeException("Agent is not published");
            }

            if (Boolean.TRUE.equals(request.getAsync())) {
                // 异步执行
                String taskId = UUID.randomUUID().toString();
                response.setTaskId(taskId);
                response.setStatus("ACCEPTED");

                // 异步执行
                executeAgentAsync(agent, request, requestId, taskId, tenantId, startTime);
            } else {
                // 同步执行
                response = executeAgentSync(agent, request, requestId, tenantId, startTime);
            }

        } catch (Exception e) {
            log.error("Error invoking agent", e);
            response.setStatus("FAILED");
            response.setErrorMessage(e.getMessage());
            response.setExecutionTime((int) (System.currentTimeMillis() - startTime));
        }

        return response;
    }

    private AgentInvokeResponse executeAgentSync(Agent agent, AgentInvokeRequest request,
                                                   String requestId, Long tenantId, long startTime) {
        AgentInvokeResponse response = new AgentInvokeResponse();
        response.setRequestId(requestId);
        long executionTime = 0;
        String errorMessage = null;
        Map<String, Object> outputs = null;

        try {
            // 使用 ExecutorService 实现执行超时控制
            ExecutorService timeoutExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "agent-timeout-" + agent.getId());
                t.setDaemon(true);
                return t;
            });
            Future<Map<String, Object>> future = timeoutExecutor.submit(
                    () -> executeAgentLogic(agent, request, tenantId));

            try {
                outputs = future.get(aiAgentProperties.getExecution().getTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("Agent {} 执行超时 ({}s)，已取消", agent.getName(), aiAgentProperties.getExecution().getTimeoutSeconds());
                throw new RuntimeException("Agent 执行超时 (" + aiAgentProperties.getExecution().getTimeoutSeconds() + "s)，请优化 Agent 配置或增大超时时间");
            } finally {
                timeoutExecutor.shutdownNow();
            }

            response.setStatus("SUCCESS");
            response.setOutputs(outputs);
            executionTime = System.currentTimeMillis() - startTime;
            response.setExecutionTime((int) executionTime);

        } catch (Exception e) {
            log.error("Error executing agent synchronously", e);
            response.setStatus("FAILED");
            errorMessage = e.getMessage();
            response.setErrorMessage(errorMessage);
            executionTime = System.currentTimeMillis() - startTime;
            response.setExecutionTime((int) executionTime);
        } finally {
            // 执行反思评估
            try {
                Map<String, Object> executionContext = new HashMap<>();
                executionContext.put("inputs", request.getInputs());
                executionContext.put("executionTime", executionTime);
                String executionResult = outputs != null ? outputs.toString() : null;
                // 修复: 从安全上下文中获取当前用户ID，不再硬编码为1L
                Long userId = SecurityUtils.getCurrentUserId();
                if (userId == null) {
                    userId = 1L; // 异步线程中可能无法获取安全上下文，使用默认值
                    log.warn("无法从安全上下文获取用户ID，使用默认值: {}", userId);
                }
                reflectionEvaluationService.evaluateExecution(agent, executionContext, executionResult,
                                                           errorMessage, executionTime, tenantId, userId);
            } catch (Exception e) {
                log.error("Error during reflection evaluation", e);
            }
        }

        return response;
    }

    @Async
    public void executeAgentAsync(Agent agent, AgentInvokeRequest request,
                                   String requestId, String taskId, Long tenantId, long startTime) {
        AgentInvokeResponse response = new AgentInvokeResponse();
        response.setRequestId(requestId);
        response.setTaskId(taskId);
        long executionTime = 0;
        String errorMessage = null;
        Map<String, Object> outputs = null;

        try {
            // 通过图执行引擎执行 Agent
            outputs = executeAgentLogic(agent, request, tenantId);

            response.setStatus("SUCCESS");
            response.setOutputs(outputs);
            executionTime = System.currentTimeMillis() - startTime;
            response.setExecutionTime((int) executionTime);

            // 如果有回调URL，发送回调
            if (request.getCallbackUrl() != null) {
                sendCallback(request.getCallbackUrl(), response);
            }

        } catch (Exception e) {
            log.error("Error executing agent asynchronously", e);
            response.setStatus("FAILED");
            errorMessage = e.getMessage();
            response.setErrorMessage(errorMessage);
            executionTime = System.currentTimeMillis() - startTime;
            response.setExecutionTime((int) executionTime);
        } finally {
            // 执行反思评估
            try {
                Map<String, Object> executionContext = new HashMap<>();
                executionContext.put("inputs", request.getInputs());
                executionContext.put("executionTime", executionTime);
                String executionResult = outputs != null ? outputs.toString() : null;
                // 修复: 从安全上下文中获取当前用户ID，不再硬编码为1L
                Long userId = SecurityUtils.getCurrentUserId();
                if (userId == null) {
                    userId = 1L; // 异步线程中可能无法获取安全上下文，使用默认值
                    log.warn("无法从安全上下文获取用户ID，使用默认值: {}", userId);
                }
                reflectionEvaluationService.evaluateExecution(agent, executionContext, executionResult,
                                                           errorMessage, executionTime, tenantId, userId);
            } catch (Exception e) {
                log.error("Error during reflection evaluation", e);
            }
        }

        // 保存任务结果到 Redis，TTL 1小时
        try {
            String redisKey = ASYNC_TASK_KEY_PREFIX + taskId;
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(redisKey, json, ASYNC_TASK_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("Error serializing async task result for taskId: {}", taskId, e);
        }
    }

    /**
     * 通过图执行引擎执行 Agent 逻辑
     *
     * 核心流程:
     * 1. 解析 Agent 配置，构建图定义
     * 2. 构建类型化的 AgentState
     * 3. 调用 GraphExecutor 执行图
     * 4. 返回执行结果
     */
    private Map<String, Object> executeAgentLogic(Agent agent, AgentInvokeRequest request, Long tenantId) {
        log.info("Executing agent via graph engine: {}", agent.getName());

        // 1. 解析 Agent 配置
        Map<String, Object> config = parseAgentConfig(agent);

        // 2. 使用 GraphParser 解析图定义（如果没有图定义，会自动创建默认线性流程）
        GraphDefinition graph = graphParser.parse(config);

        // 3. 构建类型化的 AgentState
        AgentState state = new AgentState();
        state.setAgentId(agent.getId());
        state.setTenantId(tenantId);
        // 修复: 从安全上下文中获取当前用户ID，不再硬编码为1L
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            currentUserId = 1L;
            log.warn("无法从安全上下文获取用户ID，使用默认值: {}", currentUserId);
        }
        state.setUserId(currentUserId);
        state.setExecutionId(UUID.randomUUID().toString());
        state.setInputs(request.getInputs() != null ? request.getInputs() : new HashMap<>());
        state.getProcessedInput().putAll(state.getInputs());

        // 4. 调用 GraphExecutor 执行图
        Map<String, Object> graphResult = graphExecutor.execute(graph, state);

        // 5. 构建最终输出（保持与旧格式兼容）
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Agent executed successfully with Graph Engine");
        result.put("agentName", agent.getName());
        result.put("inputs", request.getInputs());

        // 从图执行结果中提取输出
        if (graphResult.get("outputs") instanceof Map) {
            // 图执行结果中的 outputs 是 Object 类型，需要强制转换为 Map
            @SuppressWarnings("unchecked")
            Map<String, Object> outputs = (Map<String, Object>) graphResult.get("outputs");
            result.putAll(outputs);
        }

        if (graphResult.get("llmOutput") != null) {
            result.put("output", graphResult.get("llmOutput"));
            result.put("llmResponse", graphResult.get("llmOutput"));
        }

        result.put("iterations", graphResult.get("iterations"));
        result.put("graphStatus", graphResult.get("status"));

        // 记录执行日志
        log.info("Agent {} 执行完成, 迭代次数: {}, 状态: {}",
            agent.getName(), graphResult.get("iterations"), graphResult.get("status"));

        return result;
    }

    // Agent 配置从数据库 JSON 字段反序列化，值为 Object 类型，需要强制转换
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAgentConfig(Agent agent) {
        Map<String, Object> config = new HashMap<>();
        if (agent.getConfig() != null) {
            config.putAll(agent.getConfig());
        }
        // 设置默认值
        config.putIfAbsent("provider", aiAgentProperties.getLlm().getDefaultProvider());
        config.putIfAbsent("model", "gpt-4");
        config.putIfAbsent("temperature", 0.7);
        config.putIfAbsent("maxTokens", 1024);
        return config;
    }

    private void sendCallback(String callbackUrl, AgentInvokeResponse response) {
        log.info("Sending callback to: {}", callbackUrl);
        try {
            RestTemplate restTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
            factory.setConnectTimeout(10_000);
            factory.setReadTimeout(30_000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = objectMapper.writeValueAsString(response);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> callbackResponse = restTemplate.exchange(
                    callbackUrl, HttpMethod.POST, entity, String.class);
            log.info("Callback sent successfully to: {}, status: {}", callbackUrl, callbackResponse.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send callback to {}: {}", callbackUrl, e.getMessage(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    public AgentInvokeResponse getTaskStatus(String taskId) {
        String redisKey = ASYNC_TASK_KEY_PREFIX + taskId;
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            AgentInvokeResponse notFound = new AgentInvokeResponse();
            notFound.setTaskId(taskId);
            notFound.setStatus("NOT_FOUND");
            return notFound;
        }
        try {
            return objectMapper.readValue(json, AgentInvokeResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing async task result for taskId: {}", taskId, e);
            AgentInvokeResponse errorResponse = new AgentInvokeResponse();
            errorResponse.setTaskId(taskId);
            errorResponse.setStatus("ERROR");
            errorResponse.setErrorMessage("Failed to deserialize task result");
            return errorResponse;
        }
    }
}
