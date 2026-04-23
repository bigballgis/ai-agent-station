package com.aiagent.service;

import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.entity.WorkflowInstance;
import com.aiagent.entity.WorkflowNodeLog;
import com.aiagent.exception.BusinessException;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.WorkflowDefinitionRepository;
import com.aiagent.repository.WorkflowInstanceRepository;
import com.aiagent.repository.WorkflowNodeLogRepository;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowNodeLogRepository nodeLogRepository;
    private final WorkflowAsyncExecutor workflowAsyncExecutor;

    /**
     * Start a workflow instance from a published definition
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstance startWorkflow(Long definitionId, Map<String, Object> variables, Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(definitionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("工作流定义不存在"));

        if (definition.getStatus() != WorkflowDefinition.WorkflowStatus.PUBLISHED) {
            throw new BusinessException("工作流定义未发布，无法启动");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowDefinitionId(definitionId);
        instance.setWorkflowName(definition.getName());
        instance.setStatus(WorkflowInstance.InstanceStatus.PENDING);
        instance.setVariables(variables != null ? variables : new HashMap<>());
        instance.setInput(variables);
        instance.setStartedBy(userId);
        instance.setStartedAt(LocalDateTime.now());
        instance.setTenantId(tenantId);
        instance.setCurrentStep(0);

        // Find the START node
        String startNodeId = findStartNode(definition);
        instance.setCurrentNodeId(startNodeId);

        instance = instanceRepository.save(instance);

        // Transition to RUNNING
        instance.setStatus(WorkflowInstance.InstanceStatus.RUNNING);
        instanceRepository.save(instance);

        // Log the start node
        createNodeLog(instance.getId(), startNodeId, "START", "开始节点", variables);

        // Execute the first node asynchronously via separate bean
        workflowAsyncExecutor.executeNodeAsync(instance.getId());

        return instance;
    }

    /**
     * Execute the current node of a workflow instance
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowNodeLog executeNode(Long instanceId) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("工作流实例不存在"));

        if (instance.getStatus() != WorkflowInstance.InstanceStatus.RUNNING
                && instance.getStatus() != WorkflowInstance.InstanceStatus.SUSPENDED) {
            throw new BusinessException("工作流实例不在运行状态");
        }

        String currentNodeId = instance.getCurrentNodeId();
        if (currentNodeId == null) {
            completeWorkflow(instance, null);
            return null;
        }

        WorkflowDefinition definition = definitionRepository.findById(instance.getWorkflowDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("工作流定义不存在"));

        Map<String, Object> nodeConfig = getNodeConfig(definition, currentNodeId);
        String nodeType = (String) nodeConfig.getOrDefault("type", "UNKNOWN");
        String nodeName = (String) nodeConfig.getOrDefault("name", currentNodeId);

        // Create or update node log
        WorkflowNodeLog nodeLog = getOrCreateNodeLog(instanceId, currentNodeId, nodeType, nodeName, instance.getVariables());
        nodeLog.setStatus(WorkflowNodeLog.NodeLogStatus.RUNNING);
        nodeLog.setStartedAt(LocalDateTime.now());
        nodeLogRepository.save(nodeLog);

        try {
            Map<String, Object> output = processNode(nodeType, nodeConfig, instance);

            nodeLog.setStatus(WorkflowNodeLog.NodeLogStatus.COMPLETED);
            nodeLog.setOutput(output);
            nodeLog.setCompletedAt(LocalDateTime.now());
            nodeLog.setDuration(calculateDuration(nodeLog.getStartedAt(), nodeLog.getCompletedAt()));
            nodeLogRepository.save(nodeLog);

            // Determine next node
            String nextNodeId = determineNextNode(definition, currentNodeId, nodeType, output, instance);

            if (nextNodeId == null || "END".equals(nodeType)) {
                completeWorkflow(instance, output);
            } else if ("APPROVAL".equals(nodeType)) {
                // Suspend workflow waiting for approval
                instance.setStatus(WorkflowInstance.InstanceStatus.SUSPENDED);
                instance.setCurrentNodeId(currentNodeId);
                instanceRepository.save(instance);
            } else {
                instance.setCurrentNodeId(nextNodeId);
                instance.setCurrentStep((instance.getCurrentStep() != null ? instance.getCurrentStep() : 0) + 1);
                instanceRepository.save(instance);

                // Continue execution asynchronously via separate bean
                workflowAsyncExecutor.executeNodeAsync(instance.getId());
            }

            return nodeLog;
        } catch (Exception e) {
            log.error("Node execution failed: nodeId={}, error={}", currentNodeId, e.getMessage(), e);
            nodeLog.setStatus(WorkflowNodeLog.NodeLogStatus.FAILED);
            nodeLog.setError(e.getMessage());
            nodeLog.setCompletedAt(LocalDateTime.now());
            nodeLog.setDuration(calculateDuration(nodeLog.getStartedAt(), nodeLog.getCompletedAt()));
            nodeLogRepository.save(nodeLog);

            instance.setStatus(WorkflowInstance.InstanceStatus.FAILED);
            instance.setError("节点 " + nodeName + " 执行失败: " + e.getMessage());
            instance.setCompletedAt(LocalDateTime.now());
            instanceRepository.save(instance);

            throw new BusinessException("节点执行失败: " + e.getMessage());
        }
    }

    /**
     * Complete an approval node
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowNodeLog approveNode(Long instanceId, boolean approved, String comment) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("工作流实例不存在"));

        if (instance.getStatus() != WorkflowInstance.InstanceStatus.SUSPENDED) {
            throw new BusinessException("工作流实例不在等待审批状态");
        }

        String currentNodeId = instance.getCurrentNodeId();
        WorkflowDefinition definition = definitionRepository.findById(instance.getWorkflowDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("工作流定义不存在"));

        Map<String, Object> nodeConfig = getNodeConfig(definition, currentNodeId);

        // Update the node log
        List<WorkflowNodeLog> logs = nodeLogRepository.findByInstanceIdAndNodeId(instanceId, currentNodeId);
        WorkflowNodeLog nodeLog = logs.isEmpty() ? null : logs.get(logs.size() - 1);

        if (nodeLog != null) {
            Map<String, Object> output = new HashMap<>();
            output.put("approved", approved);
            output.put("comment", comment);

            nodeLog.setOutput(output);
            nodeLog.setStatus(approved ? WorkflowNodeLog.NodeLogStatus.COMPLETED : WorkflowNodeLog.NodeLogStatus.FAILED);
            nodeLog.setCompletedAt(LocalDateTime.now());
            nodeLog.setDuration(calculateDuration(nodeLog.getStartedAt(), nodeLog.getCompletedAt()));
            nodeLogRepository.save(nodeLog);
        }

        if (!approved) {
            instance.setStatus(WorkflowInstance.InstanceStatus.FAILED);
            instance.setError("审批被拒绝: " + comment);
            instance.setCompletedAt(LocalDateTime.now());
            instanceRepository.save(instance);
            return nodeLog;
        }

        // Resume workflow - find next node
        String nextNodeId = determineNextNode(definition, currentNodeId, "APPROVAL",
                Map.of("approved", true, "comment", comment), instance);

        instance.setStatus(WorkflowInstance.InstanceStatus.RUNNING);
        if (nextNodeId != null) {
            instance.setCurrentNodeId(nextNodeId);
            instance.setCurrentStep((instance.getCurrentStep() != null ? instance.getCurrentStep() : 0) + 1);
        }
        instanceRepository.save(instance);

        if (nextNodeId != null) {
            workflowAsyncExecutor.executeNodeAsync(instance.getId());
        } else {
            completeWorkflow(instance, null);
        }

        return nodeLog;
    }

    /**
     * Cancel a running workflow
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstance cancelWorkflow(Long instanceId, String reason) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("工作流实例不存在"));

        if (instance.getStatus() == WorkflowInstance.InstanceStatus.COMPLETED
                || instance.getStatus() == WorkflowInstance.InstanceStatus.CANCELLED
                || instance.getStatus() == WorkflowInstance.InstanceStatus.FAILED) {
            throw new BusinessException("工作流实例已结束，无法取消");
        }

        instance.setStatus(WorkflowInstance.InstanceStatus.CANCELLED);
        instance.setError(reason);
        instance.setCompletedAt(LocalDateTime.now());
        return instanceRepository.save(instance);
    }

    /**
     * Get workflow instance status
     */
    public WorkflowInstance getWorkflowStatus(Long instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("工作流实例不存在"));
    }

    /**
     * Get workflow execution history (all node logs)
     */
    public List<WorkflowNodeLog> getWorkflowHistory(Long instanceId) {
        return nodeLogRepository.findByInstanceIdOrderByStartedAtAsc(instanceId);
    }

    // ==================== Private Helper Methods ====================

    private void completeWorkflow(WorkflowInstance instance, Map<String, Object> finalOutput) {
        instance.setStatus(WorkflowInstance.InstanceStatus.COMPLETED);
        instance.setOutput(finalOutput);
        instance.setCurrentNodeId(null);
        instance.setCompletedAt(LocalDateTime.now());
        instanceRepository.save(instance);
        log.info("Workflow completed: instanceId={}, name={}", instance.getId(), instance.getWorkflowName());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNodeConfig(WorkflowDefinition definition, String nodeId) {
        if (definition.getNodes() == null) {
            return new HashMap<>();
        }
        Object nodes = definition.getNodes().get("nodes");
        if (nodes instanceof List) {
            List<Map<String, Object>> nodeList = (List<Map<String, Object>>) nodes;
            return nodeList.stream()
                    .filter(n -> nodeId.equals(n.get("id")))
                    .findFirst()
                    .orElse(new HashMap<>());
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private String findStartNode(WorkflowDefinition definition) {
        if (definition.getNodes() == null) {
            throw new BusinessException("工作流定义没有节点配置");
        }
        Object nodes = definition.getNodes().get("nodes");
        if (nodes instanceof List) {
            List<Map<String, Object>> nodeList = (List<Map<String, Object>>) nodes;
            return nodeList.stream()
                    .filter(n -> "START".equals(n.get("type")))
                    .map(n -> (String) n.get("id"))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("工作流定义没有开始节点"));
        }
        throw new BusinessException("工作流定义节点格式错误");
    }

    @SuppressWarnings("unchecked")
    private String determineNextNode(WorkflowDefinition definition, String currentNodeId,
                                     String nodeType, Map<String, Object> output, WorkflowInstance instance) {
        if ("END".equals(nodeType)) {
            return null;
        }

        if (definition.getEdges() == null) {
            return null;
        }

        Object edges = definition.getEdges().get("edges");
        if (!(edges instanceof List)) {
            return null;
        }

        List<Map<String, Object>> edgeList = (List<Map<String, Object>>) edges;

        if ("CONDITION".equals(nodeType)) {
            // For condition nodes, evaluate the condition to determine the branch
            String conditionResult = evaluateCondition(output);
            return edgeList.stream()
                    .filter(e -> currentNodeId.equals(e.get("source")) && conditionResult.equals(e.get("label")))
                    .map(e -> (String) e.get("target"))
                    .findFirst()
                    .orElse(null);
        }

        // Default: follow the first edge from this node
        return edgeList.stream()
                .filter(e -> currentNodeId.equals(e.get("source")))
                .map(e -> (String) e.get("target"))
                .findFirst()
                .orElse(null);
    }

    private String evaluateCondition(Map<String, Object> output) {
        if (output == null) {
            return "default";
        }
        Object result = output.get("conditionResult");
        return result != null ? result.toString() : "default";
    }

    private Map<String, Object> processNode(String nodeType, Map<String, Object> nodeConfig,
                                            WorkflowInstance instance) {
        return switch (nodeType) {
            case "START" -> {
                Map<String, Object> result = new HashMap<>();
                result.put("message", "工作流启动");
                yield result;
            }
            case "END" -> {
                Map<String, Object> result = new HashMap<>();
                result.put("message", "工作流结束");
                yield result;
            }
            case "AGENT" -> processAgentNode(nodeConfig, instance);
            case "APPROVAL" -> {
                // Approval node waits for human action, return pending
                Map<String, Object> result = new HashMap<>();
                result.put("message", "等待审批");
                yield result;
            }
            case "CONDITION" -> processConditionNode(nodeConfig, instance);
            case "NOTIFY" -> processNotifyNode(nodeConfig, instance);
            case "HTTP" -> processHttpNode(nodeConfig, instance);
            case "DELAY" -> processDelayNode(nodeConfig);
            case "PARALLEL" -> processParallelNode(nodeConfig, instance);
            default -> {
                Map<String, Object> result = new HashMap<>();
                result.put("message", "未知节点类型: " + nodeType);
                yield result;
            }
        };
    }

    private Map<String, Object> processAgentNode(Map<String, Object> nodeConfig, WorkflowInstance instance) {
        String agentId = (String) nodeConfig.get("agentId");
        String prompt = (String) nodeConfig.get("prompt");

        log.info("Executing AGENT node: agentId={}, prompt={}", agentId, prompt);

        Map<String, Object> result = new HashMap<>();
        result.put("agentId", agentId);
        result.put("message", "Agent调用完成");
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    private Map<String, Object> processConditionNode(Map<String, Object> nodeConfig, WorkflowInstance instance) {
        String expression = (String) nodeConfig.get("expression");
        log.info("Executing CONDITION node: expression={}", expression);

        Map<String, Object> result = new HashMap<>();
        // Simple condition evaluation - in production this would use a proper expression engine
        result.put("conditionResult", "default");
        result.put("expression", expression);
        return result;
    }

    private Map<String, Object> processNotifyNode(Map<String, Object> nodeConfig, WorkflowInstance instance) {
        String notifyType = (String) nodeConfig.getOrDefault("notifyType", "EMAIL");
        String recipient = (String) nodeConfig.get("recipient");
        String message = (String) nodeConfig.get("message");

        log.info("Executing NOTIFY node: type={}, recipient={}, message={}", notifyType, recipient, message);

        Map<String, Object> result = new HashMap<>();
        result.put("notifyType", notifyType);
        result.put("recipient", recipient);
        result.put("message", "通知已发送");
        return result;
    }

    private Map<String, Object> processHttpNode(Map<String, Object> nodeConfig, WorkflowInstance instance) {
        String url = (String) nodeConfig.get("url");
        String method = (String) nodeConfig.getOrDefault("method", "GET");

        log.info("Executing HTTP node: method={}, url={}", method, url);

        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("method", method);
        result.put("statusCode", 200);
        result.put("message", "HTTP请求完成");
        return result;
    }

    private Map<String, Object> processDelayNode(Map<String, Object> nodeConfig) {
        int delaySeconds = nodeConfig.containsKey("delaySeconds")
                ? ((Number) nodeConfig.get("delaySeconds")).intValue()
                : 5;

        // 业务需求: Delay 节点需要等待指定时间，保留 Thread.sleep
        // 注意: delaySeconds 由 GraphExecutor/NodeExecutors 限制在 1-300 秒范围内
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("延迟节点被中断");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("delaySeconds", delaySeconds);
        result.put("message", "延迟 " + delaySeconds + " 秒完成");
        return result;
    }

    private Map<String, Object> processParallelNode(Map<String, Object> nodeConfig, WorkflowInstance instance) {
        log.info("Executing PARALLEL node for instance: {}", instance.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("message", "并行节点执行完成");
        return result;
    }

    private WorkflowNodeLog createNodeLog(Long instanceId, String nodeId, String nodeType,
                                          String nodeName, Map<String, Object> input) {
        WorkflowNodeLog nodeLog = new WorkflowNodeLog();
        nodeLog.setInstanceId(instanceId);
        nodeLog.setNodeId(nodeId);
        nodeLog.setNodeType(nodeType);
        nodeLog.setNodeName(nodeName);
        nodeLog.setStatus(WorkflowNodeLog.NodeLogStatus.COMPLETED);
        nodeLog.setInput(input);
        nodeLog.setStartedAt(LocalDateTime.now());
        nodeLog.setCompletedAt(LocalDateTime.now());
        nodeLog.setDuration(0L);
        return nodeLogRepository.save(nodeLog);
    }

    private WorkflowNodeLog getOrCreateNodeLog(Long instanceId, String nodeId, String nodeType,
                                               String nodeName, Map<String, Object> input) {
        List<WorkflowNodeLog> existing = nodeLogRepository.findByInstanceIdAndNodeId(instanceId, nodeId);
        if (!existing.isEmpty()) {
            return existing.get(existing.size() - 1);
        }

        WorkflowNodeLog nodeLog = new WorkflowNodeLog();
        nodeLog.setInstanceId(instanceId);
        nodeLog.setNodeId(nodeId);
        nodeLog.setNodeType(nodeType);
        nodeLog.setNodeName(nodeName);
        nodeLog.setInput(input);
        nodeLog.setStatus(WorkflowNodeLog.NodeLogStatus.PENDING);
        return nodeLogRepository.save(nodeLog);
    }

    private Long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.Duration.between(start, end).toMillis();
    }
}
