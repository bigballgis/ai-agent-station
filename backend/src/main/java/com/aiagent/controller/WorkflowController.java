package com.aiagent.controller;

import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;

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
import com.aiagent.vo.WorkflowDefinitionVO;
import com.aiagent.vo.WorkflowInstanceVO;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "工作流管理", description = "工作流管理接口")
public class WorkflowController {

    private final WorkflowEngine workflowEngine;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceRepository instanceRepository;

    // ==================== Workflow Definition APIs ====================

    @RequiresPermission("workflow:view")
    @GetMapping("/definitions")
    public Result<PageResult<WorkflowDefinitionVO>> listDefinitions(
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

        Page<WorkflowDefinitionVO> voPage = result.map(WorkflowDefinitionVO::fromEntity);
        return Result.success(PageResult.from(voPage));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/definitions")
    @Operation(summary = "分页查询工作流定义列表")
    public Result<WorkflowDefinitionVO> createDefinition(
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

        return Result.success(WorkflowDefinitionVO.fromEntity(definitionRepository.save(definition)));
    }

    @RequiresPermission("workflow:view")
    @GetMapping("/definitions/{id}")
    public Result<WorkflowDefinitionVO> getDefinition(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new com.aiagent.exception.BusinessException("工作流定义不存在"));
        return Result.success(WorkflowDefinitionVO.fromEntity(definition));
    }

    @Operation(summary = "根据ID获取工作流定义详情")
    @RequiresPermission("workflow:manage")
    @PutMapping("/definitions/{id}")
    public Result<WorkflowDefinitionVO> updateDefinition(
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

        return Result.success(WorkflowDefinitionVO.fromEntity(definitionRepository.save(definition)));
    }

    @RequiresPermission("workflow:manage")
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

    @Operation(summary = "删除工作流定义")
    @RequiresPermission("workflow:manage")
    @PostMapping("/definitions/{id}/publish")
    public Result<WorkflowDefinitionVO> publishDefinition(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new com.aiagent.exception.BusinessException("工作流定义不存在"));

        if (definition.getStatus() == WorkflowDefinition.WorkflowStatus.PUBLISHED) {
            throw new com.aiagent.exception.BusinessException("工作流定义已发布");
        }

        definition.setStatus(WorkflowDefinition.WorkflowStatus.PUBLISHED);
        return Result.success(WorkflowDefinitionVO.fromEntity(definitionRepository.save(definition)));
    }

    // ==================== Workflow Instance APIs ====================

    @Operation(summary = "发布工作流定义")
    @RequiresPermission("workflow:view")
    @GetMapping("/instances")
    public Result<PageResult<WorkflowInstanceVO>> listInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long definitionId) {

        Long tenantId = TenantContextHolder.getTenantId();
        Sort sort = Sort.by("startedAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowInstance> result;
        if (definitionId != null) {
            result = instanceRepository.findByWorkflowDefinitionIdAndTenantId(definitionId, tenantId, pageable);
        } else if (status != null && !status.isEmpty()) {
            result = instanceRepository.findByTenantIdAndStatus(tenantId,
                    WorkflowInstance.InstanceStatus.valueOf(status), pageable);
        } else {
            result = instanceRepository.findByTenantId(tenantId, pageable);
        }

        Page<WorkflowInstanceVO> voPage = result.map(WorkflowInstanceVO::fromEntity);
        return Result.success(PageResult.from(voPage));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/start")
    @OperationLog(value = "启动工作流", module = "工作流")
    @Operation(summary = "分页查询工作流实例列表")
    public Result<WorkflowInstanceVO> startWorkflow(
            @Valid @RequestBody StartWorkflowRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkflowInstance instance = workflowEngine.startWorkflow(
                request.getDefinitionId(),
                request.getVariables(),
                principal.getId()
        );
        return Result.success(WorkflowInstanceVO.fromEntity(instance));
    }

    @RequiresPermission("workflow:view")
    @GetMapping("/instances/{id}")
    public Result<WorkflowInstanceVO> getInstance(@PathVariable Long id) {
        WorkflowInstance instance = workflowEngine.getWorkflowStatus(id);
        return Result.success(WorkflowInstanceVO.fromEntity(instance));
    }

    @Operation(summary = "获取工作流实例详情")
    @RequiresPermission("workflow:view")
    @GetMapping("/instances/{id}/history")
    public Result<List<WorkflowNodeLog>> getInstanceHistory(@PathVariable Long id) {
        List<WorkflowNodeLog> history = workflowEngine.getWorkflowHistory(id);
        return Result.success(history);
    }

    @Operation(summary = "获取工作流实例执行历史")
    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/{id}/cancel")
    @OperationLog(value = "取消工作流", module = "工作流")
    public Result<WorkflowInstanceVO> cancelWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody CancelWorkflowRequest request) {

        WorkflowInstance instance = workflowEngine.cancelWorkflow(id, request.getReason());
        return Result.success(WorkflowInstanceVO.fromEntity(instance));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/{instanceId}/nodes/{nodeId}/approve")
    @OperationLog(value = "审批工作流节点", module = "工作流")
    @Operation(summary = "取消工作流")
    public Result<WorkflowNodeLog> approveNode(
            @PathVariable Long instanceId,
            @PathVariable String nodeId,
            @RequestBody(required = false) NodeActionRequest request) {

        String comment = request != null ? request.getComment() : "";
        WorkflowNodeLog nodeLog = workflowEngine.approveNode(instanceId, true, comment);
        return Result.success(nodeLog);
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/{instanceId}/nodes/{nodeId}/reject")
    @Operation(summary = "审批工作流节点")
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
