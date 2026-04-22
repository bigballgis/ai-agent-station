package com.aiagent.controller;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.entity.WorkflowInstance;
import com.aiagent.entity.WorkflowNodeLog;
import com.aiagent.repository.WorkflowDefinitionRepository;
import com.aiagent.repository.WorkflowInstanceRepository;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.WorkflowEngine;
import com.aiagent.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowEngine workflowEngine;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceRepository instanceRepository;

    // ==================== Workflow Definition APIs ====================

    @GetMapping("/definitions")
    public Result<PageResult<WorkflowDefinition>> listDefinitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Long tenantId = TenantContextHolder.getTenantId();
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowDefinition> result;
        if (status != null && !status.isEmpty()) {
            result = definitionRepository.findByTenantIdAndStatus(tenantId,
                    WorkflowDefinition.WorkflowStatus.valueOf(status), pageable);
        } else {
            result = definitionRepository.findByTenantId(tenantId, pageable);
        }

        return Result.success(PageResult.from(result));
    }

    @PostMapping("/definitions")
    public Result<WorkflowDefinition> createDefinition(
            @Valid @RequestBody CreateDefinitionRequest request) {

        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName(request.getName());
        definition.setDescription(request.getDescription());
        definition.setVersion(1);
        definition.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        definition.setNodes(request.getNodes());
        definition.setEdges(request.getEdges());
        definition.setTriggers(request.getTriggers());
        definition.setTenantId(tenantId);

        return Result.success(definitionRepository.save(definition));
    }

    @GetMapping("/definitions/{id}")
    public Result<WorkflowDefinition> getDefinition(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new com.aiagent.exception.BusinessException("工作流定义不存在"));
        return Result.success(definition);
    }

    @PutMapping("/definitions/{id}")
    public Result<WorkflowDefinition> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDefinitionRequest request) {

        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new com.aiagent.exception.BusinessException("工作流定义不存在"));

        if (definition.getStatus() == WorkflowDefinition.WorkflowStatus.PUBLISHED) {
            throw new com.aiagent.exception.BusinessException("已发布的工作流定义不能直接修改，请先创建新版本");
        }

        if (request.getName() != null) {
            definition.setName(request.getName());
        }
        if (request.getDescription() != null) {
            definition.setDescription(request.getDescription());
        }
        if (request.getNodes() != null) {
            definition.setNodes(request.getNodes());
        }
        if (request.getEdges() != null) {
            definition.setEdges(request.getEdges());
        }
        if (request.getTriggers() != null) {
            definition.setTriggers(request.getTriggers());
        }

        return Result.success(definitionRepository.save(definition));
    }

    @DeleteMapping("/definitions/{id}")
    public Result<Void> deleteDefinition(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new com.aiagent.exception.BusinessException("工作流定义不存在"));

        if (definition.getStatus() == WorkflowDefinition.WorkflowStatus.PUBLISHED) {
            throw new com.aiagent.exception.BusinessException("已发布的工作流定义不能删除，请先归档");
        }

        definitionRepository.delete(definition);
        return Result.success();
    }

    @PostMapping("/definitions/{id}/publish")
    public Result<WorkflowDefinition> publishDefinition(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new com.aiagent.exception.BusinessException("工作流定义不存在"));

        if (definition.getStatus() == WorkflowDefinition.WorkflowStatus.PUBLISHED) {
            throw new com.aiagent.exception.BusinessException("工作流定义已发布");
        }

        definition.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        return Result.success(definitionRepository.save(definition));
    }

    // ==================== Workflow Instance APIs ====================

    @GetMapping("/instances")
    public Result<PageResult<WorkflowInstance>> listInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long definitionId) {

        Long tenantId = TenantContextHolder.getTenantId();
        Sort sort = Sort.by("startedAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowInstance> result;
        if (definitionId != null) {
            result = instanceRepository.findByWorkflowDefinitionIdAndTenantId(definitionId, tenantId)
                    .stream().collect(java.util.stream.Collectors.toList())
                    .stream().skip((long) page * size).limit(size)
                    .collect(java.util.stream.Collectors.toList())
                    .stream().collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toList(),
                            list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())));
            // Simplified pagination for list query
            List<WorkflowInstance> all = instanceRepository.findByWorkflowDefinitionIdAndTenantId(definitionId, tenantId);
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            List<WorkflowInstance> content = all.subList(fromIndex, toIndex);
            result = new org.springframework.data.domain.PageImpl<>(content, pageable, total);
        } else if (status != null && !status.isEmpty()) {
            List<WorkflowInstance> all = instanceRepository.findByTenantIdAndStatus(tenantId,
                    WorkflowInstance.InstanceStatus.valueOf(status));
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            List<WorkflowInstance> content = all.subList(fromIndex, toIndex);
            result = new org.springframework.data.domain.PageImpl<>(content, pageable, total);
        } else {
            result = instanceRepository.findByTenantId(tenantId, pageable);
        }

        return Result.success(PageResult.from(result));
    }

    @PostMapping("/instances/start")
    public Result<WorkflowInstance> startWorkflow(
            @Valid @RequestBody StartWorkflowRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkflowInstance instance = workflowEngine.startWorkflow(
                request.getDefinitionId(),
                request.getVariables(),
                principal.getId()
        );
        return Result.success(instance);
    }

    @GetMapping("/instances/{id}")
    public Result<WorkflowInstance> getInstance(@PathVariable Long id) {
        WorkflowInstance instance = workflowEngine.getWorkflowStatus(id);
        return Result.success(instance);
    }

    @GetMapping("/instances/{id}/history")
    public Result<List<WorkflowNodeLog>> getInstanceHistory(@PathVariable Long id) {
        List<WorkflowNodeLog> history = workflowEngine.getWorkflowHistory(id);
        return Result.success(history);
    }

    @PostMapping("/instances/{id}/cancel")
    public Result<WorkflowInstance> cancelWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody CancelWorkflowRequest request) {

        WorkflowInstance instance = workflowEngine.cancelWorkflow(id, request.getReason());
        return Result.success(instance);
    }

    @PostMapping("/instances/{instanceId}/nodes/{nodeId}/approve")
    public Result<WorkflowNodeLog> approveNode(
            @PathVariable Long instanceId,
            @PathVariable String nodeId,
            @RequestBody(required = false) NodeActionRequest request) {

        String comment = request != null ? request.getComment() : "";
        WorkflowNodeLog nodeLog = workflowEngine.approveNode(instanceId, true, comment);
        return Result.success(nodeLog);
    }

    @PostMapping("/instances/{instanceId}/nodes/{nodeId}/reject")
    public Result<WorkflowNodeLog> rejectNode(
            @PathVariable Long instanceId,
            @PathVariable String nodeId,
            @RequestBody(required = false) NodeActionRequest request) {

        String comment = request != null ? request.getComment() : "";
        WorkflowNodeLog nodeLog = workflowEngine.approveNode(instanceId, false, comment);
        return Result.success(nodeLog);
    }

    // ==================== Request DTOs ====================

    @Data
    public static class CreateDefinitionRequest {
        private String name;
        private String description;
        private Map<String, Object> nodes;
        private Map<String, Object> edges;
        private Map<String, Object> triggers;
    }

    @Data
    public static class UpdateDefinitionRequest {
        private String name;
        private String description;
        private Map<String, Object> nodes;
        private Map<String, Object> edges;
        private Map<String, Object> triggers;
    }

    @Data
    public static class StartWorkflowRequest {
        private Long definitionId;
        private Map<String, Object> variables;
    }

    @Data
    public static class CancelWorkflowRequest {
        private String reason;
    }

    @Data
    public static class NodeActionRequest {
        private String comment;
    }
}
