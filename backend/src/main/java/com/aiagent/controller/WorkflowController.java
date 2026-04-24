package com.aiagent.controller;

import com.aiagent.annotation.Audited;
import com.aiagent.annotation.AuditAction;
import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.WorkflowCancelDTO;
import com.aiagent.dto.WorkflowDefinitionDTO;
import com.aiagent.dto.WorkflowNodeActionDTO;
import com.aiagent.dto.WorkflowStartDTO;
import com.aiagent.entity.WorkflowDefinition;
import com.aiagent.entity.WorkflowInstance;
import com.aiagent.entity.WorkflowNodeLog;
import com.aiagent.exception.BusinessException;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.WorkflowEngine;
import com.aiagent.service.WorkflowService;
import com.aiagent.tenant.TenantContextHolder;
import com.aiagent.vo.WorkflowDefinitionVO;
import com.aiagent.vo.WorkflowInstanceVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "工作流管理", description = "工作流管理接口")
public class WorkflowController {

    private final WorkflowEngine workflowEngine;
    private final WorkflowService workflowService;

    // ==================== Workflow Definition APIs ====================

    @RequiresPermission("workflow:view")
    @GetMapping("/definitions")
    @Operation(summary = "分页查询工作流定义列表")
    public Result<PageResult<WorkflowDefinitionVO>> listDefinitions(
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) @Parameter(description = "每页大小") int size,
            @RequestParam(required = false) @Parameter(description = "状态筛选") String status) {

        Long tenantId = TenantContextHolder.getTenantId();
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowDefinition> result;
        if (status != null && !status.isEmpty()) {
            result = workflowService.listDefinitionsByStatus(tenantId,
                    WorkflowDefinition.WorkflowStatus.valueOf(status), pageable);
        } else {
            result = workflowService.listDefinitions(tenantId, pageable);
        }

        Page<WorkflowDefinitionVO> voPage = result.map(WorkflowDefinitionVO::fromEntity);
        return Result.success(PageResult.from(voPage));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/definitions")
    @Operation(summary = "创建工作流定义", description = "创建新的工作流定义，初始状态为草稿")
    @Audited(action = AuditAction.CREATE, module = "工作流", description = "创建工作流定义", resourceType = "WorkflowDefinition")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<WorkflowDefinitionVO> createDefinition(
            @Valid @RequestBody WorkflowDefinitionDTO request) {

        Long tenantId = TenantContextHolder.getTenantId();

        // 验证节点数量限制
        workflowService.validateNodeCount(request.getNodes());

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName(request.getName());
        definition.setDescription(request.getDescription());
        definition.setVersion(1);
        definition.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        definition.setNodes(request.getNodes());
        definition.setEdges(request.getEdges());
        definition.setTriggers(request.getTriggers());
        definition.setTenantId(tenantId);

        WorkflowDefinition saved = workflowService.createDefinition(definition);
        // 首次创建时 baseDefinitionId 等于自身 ID
        saved.setBaseDefinitionId(saved.getId());
        saved = workflowService.updateDefinition(saved);

        return Result.success(WorkflowDefinitionVO.fromEntity(saved));
    }

    @RequiresPermission("workflow:view")
    @GetMapping("/definitions/{id}")
    @Operation(summary = "根据ID获取工作流定义详情")
    public Result<WorkflowDefinitionVO> getDefinition(@Parameter(description = "工作流定义ID") @PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        WorkflowDefinition definition = workflowService.getDefinitionByIdAndTenantId(id, tenantId);
        return Result.success(WorkflowDefinitionVO.fromEntity(definition));
    }

    @RequiresPermission("workflow:manage")
    @PutMapping("/definitions/{id}")
    @Operation(summary = "更新工作流定义")
    @Audited(action = AuditAction.UPDATE, module = "工作流", description = "更新工作流定义", resourceType = "WorkflowDefinition")
    public Result<WorkflowDefinitionVO> updateDefinition(
            @Parameter(description = "工作流定义ID") @PathVariable Long id,
            @Valid @RequestBody WorkflowDefinitionDTO request) {

        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = workflowService.getDefinitionByIdAndTenantId(id, tenantId);

        if (definition.getStatus() == WorkflowDefinition.WorkflowStatus.PUBLISHED) {
            throw new com.aiagent.exception.BusinessException("已发布的工作流定义不能直接修改，请先创建新版本");
        }

        // 验证节点数量限制
        if (request.getNodes() != null) {
            workflowService.validateNodeCount(request.getNodes());
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

        return Result.success(WorkflowDefinitionVO.fromEntity(workflowService.updateDefinition(definition)));
    }

    @RequiresPermission("workflow:manage")
    @DeleteMapping("/definitions/{id}")
    @Operation(summary = "删除工作流定义")
    @Audited(action = AuditAction.DELETE, module = "工作流", description = "删除工作流定义", resourceType = "WorkflowDefinition")
    public Result<Void> deleteDefinition(@Parameter(description = "工作流定义ID") @PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = workflowService.getDefinitionByIdAndTenantId(id, tenantId);

        if (definition.getStatus() == WorkflowDefinition.WorkflowStatus.PUBLISHED) {
            throw new com.aiagent.exception.BusinessException("已发布的工作流定义不能删除，请先归档");
        }

        workflowService.deleteDefinition(definition);
        return Result.success();
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/definitions/{id}/publish")
    @Operation(summary = "发布工作流定义")
    @Audited(action = AuditAction.UPDATE, module = "工作流", description = "发布工作流定义", resourceType = "WorkflowDefinition")
    public Result<WorkflowDefinitionVO> publishDefinition(@Parameter(description = "工作流定义ID") @PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();

        WorkflowDefinition definition = workflowService.getDefinitionByIdAndTenantId(id, tenantId);

        if (definition.getStatus() == WorkflowDefinition.WorkflowStatus.PUBLISHED) {
            throw new com.aiagent.exception.BusinessException("工作流定义已发布");
        }

        return Result.success(WorkflowDefinitionVO.fromEntity(workflowService.publishDefinition(definition)));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/definitions/{id}/new-version")
    @Operation(summary = "基于当前定义创建新版本（草稿）")
    public Result<WorkflowDefinitionVO> createNewVersion(@Parameter(description = "工作流定义ID") @PathVariable Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        WorkflowDefinition newVersion = workflowService.createNewVersion(id, tenantId);
        return Result.success(WorkflowDefinitionVO.fromEntity(newVersion));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/definitions/{id}/rollback/{targetVersion}")
    @Operation(summary = "回滚工作流到指定版本（创建新草稿）")
    public Result<WorkflowDefinitionVO> rollbackToVersion(
            @Parameter(description = "工作流定义ID") @PathVariable Long id,
            @Parameter(description = "目标版本号") @PathVariable Integer targetVersion) {
        Long tenantId = TenantContextHolder.getTenantId();
        WorkflowDefinition rollbackDraft = workflowService.rollbackToVersion(id, targetVersion, tenantId);
        return Result.success(WorkflowDefinitionVO.fromEntity(rollbackDraft));
    }

    // ==================== Workflow Instance APIs ====================

    @RequiresPermission("workflow:view")
    @GetMapping("/instances")
    @Operation(summary = "分页查询工作流实例列表")
    public Result<PageResult<WorkflowInstanceVO>> listInstances(
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) @Parameter(description = "每页大小") int size,
            @RequestParam(required = false) @Parameter(description = "状态筛选") String status,
            @RequestParam(required = false) @Parameter(description = "工作流定义ID") Long definitionId) {

        Long tenantId = TenantContextHolder.getTenantId();
        Sort sort = Sort.by("startedAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowInstance> result;
        if (definitionId != null) {
            result = workflowService.listInstancesByDefinitionId(definitionId, tenantId, pageable);
        } else if (status != null && !status.isEmpty()) {
            result = workflowService.listInstancesByStatus(tenantId,
                    WorkflowInstance.InstanceStatus.valueOf(status), pageable);
        } else {
            result = workflowService.listInstances(tenantId, pageable);
        }

        Page<WorkflowInstanceVO> voPage = result.map(WorkflowInstanceVO::fromEntity);
        return Result.success(PageResult.from(voPage));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/start")
    @OperationLog(value = "启动工作流", module = "工作流")
    @Operation(summary = "启动工作流", description = "根据工作流定义启动一个新的工作流实例")
    @Audited(action = AuditAction.CREATE, module = "工作流", description = "启动工作流", resourceType = "WorkflowInstance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "启动成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败或工作流未发布"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "工作流定义不存在")
    })
    public Result<WorkflowInstanceVO> startWorkflow(
            @Valid @RequestBody WorkflowStartDTO request,
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
    @Operation(summary = "获取工作流实例详情")
    public Result<WorkflowInstanceVO> getInstance(@Parameter(description = "工作流实例ID") @PathVariable Long id) {
        WorkflowInstance instance = workflowEngine.getWorkflowStatus(id);
        return Result.success(WorkflowInstanceVO.fromEntity(instance));
    }

    @RequiresPermission("workflow:view")
    @GetMapping("/instances/{id}/history")
    @Operation(summary = "获取工作流实例执行历史")
    public Result<List<WorkflowNodeLog>> getInstanceHistory(@Parameter(description = "工作流实例ID") @PathVariable Long id) {
        List<WorkflowNodeLog> history = workflowEngine.getWorkflowHistory(id);
        return Result.success(history);
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/{id}/cancel")
    @OperationLog(value = "取消工作流", module = "工作流")
    @Operation(summary = "取消工作流")
    @Audited(action = AuditAction.UPDATE, module = "工作流", description = "取消工作流", resourceType = "WorkflowInstance")
    public Result<WorkflowInstanceVO> cancelWorkflow(
            @Parameter(description = "工作流实例ID") @PathVariable Long id,
            @Valid @RequestBody WorkflowCancelDTO request) {

        WorkflowInstance instance = workflowEngine.cancelWorkflow(id, request.getReason());
        return Result.success(WorkflowInstanceVO.fromEntity(instance));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/{id}/resume")
    @OperationLog(value = "恢复工作流", module = "工作流")
    @Operation(summary = "恢复中断的工作流实例")
    @Audited(action = AuditAction.UPDATE, module = "工作流", description = "恢复工作流", resourceType = "WorkflowInstance")
    public Result<WorkflowInstanceVO> resumeWorkflow(@Parameter(description = "工作流实例ID") @PathVariable Long id) {
        WorkflowInstance instance = workflowEngine.resumeWorkflow(id);
        return Result.success(WorkflowInstanceVO.fromEntity(instance));
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/{instanceId}/nodes/{nodeId}/approve")
    @OperationLog(value = "审批工作流节点", module = "工作流")
    @Operation(summary = "审批通过工作流节点")
    @Audited(action = AuditAction.APPROVE, module = "工作流", description = "审批通过工作流节点", resourceType = "WorkflowInstance")
    public Result<WorkflowNodeLog> approveNode(
            @Parameter(description = "工作流实例ID") @PathVariable Long instanceId,
            @Parameter(description = "节点ID") @PathVariable String nodeId,
            @Valid @RequestBody(required = false) WorkflowNodeActionDTO request) {

        String comment = request != null ? request.getComment() : "";
        WorkflowNodeLog nodeLog = workflowEngine.approveNode(instanceId, true, comment);
        return Result.success(nodeLog);
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/instances/{instanceId}/nodes/{nodeId}/reject")
    @Operation(summary = "驳回工作流节点")
    @Audited(action = AuditAction.REJECT, module = "工作流", description = "驳回工作流节点", resourceType = "WorkflowInstance")
    public Result<WorkflowNodeLog> rejectNode(
            @Parameter(description = "工作流实例ID") @PathVariable Long instanceId,
            @Parameter(description = "节点ID") @PathVariable String nodeId,
            @Valid @RequestBody(required = false) WorkflowNodeActionDTO request) {

        String comment = request != null ? request.getComment() : "";
        WorkflowNodeLog nodeLog = workflowEngine.approveNode(instanceId, false, comment);
        return Result.success(nodeLog);
    }

    // ==================== 导出/导入接口 ====================

    @RequiresPermission("workflow:view")
    @GetMapping("/definitions/{id}/export")
    @Operation(summary = "导出工作流定义为JSON")
    public void exportDefinition(
            @Parameter(description = "工作流定义ID") @PathVariable Long id,
            HttpServletResponse response) throws Exception {
        Long tenantId = TenantContextHolder.getTenantId();
        WorkflowDefinition definition = workflowService.getDefinitionByIdAndTenantId(id, tenantId);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("name", definition.getName());
        exportData.put("description", definition.getDescription());
        exportData.put("version", definition.getVersion());
        exportData.put("nodes", definition.getNodes());
        exportData.put("edges", definition.getEdges());
        exportData.put("triggers", definition.getTriggers());

        String filename = definition.getName().replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_");
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "_" + timestamp + ".json\"");
        response.setCharacterEncoding("UTF-8");
        mapper.writerWithDefaultPrettyPrinter().writeValue(response.getOutputStream(), exportData);
    }

    @RequiresPermission("workflow:manage")
    @PostMapping("/definitions/import")
    @Operation(summary = "从JSON导入工作流定义", description = "导入工作流定义，自动处理名称冲突")
    @OperationLog(value = "导入工作流", module = "工作流")
    @Audited(action = AuditAction.IMPORT, module = "工作流", description = "导入工作流定义", resourceType = "WorkflowDefinition")
    public Result<WorkflowDefinitionVO> importDefinition(@RequestBody Map<String, Object> data) {
        // 验证必填字段
        if (data.get("name") == null || String.valueOf(data.get("name")).isBlank()) {
            throw new BusinessException("导入数据缺少必填字段: name");
        }

        String name = String.valueOf(data.get("name"));

        // 验证图结构
        @SuppressWarnings("unchecked")
        Map<String, Object> nodes = data.get("nodes") instanceof Map ? (Map<String, Object>) data.get("nodes") : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> edges = data.get("edges") instanceof Map ? (Map<String, Object>) data.get("edges") : null;
        workflowService.validateNodeCount(nodes);
        workflowService.validateEdgeCount(edges);

        Long tenantId = TenantContextHolder.getTenantId();

        // 处理名称冲突：自动添加后缀
        String originalName = name;
        int suffix = 1;
        while (workflowService.existsByNameAndTenantId(name, tenantId)) {
            name = originalName + " (" + suffix + ")";
            suffix++;
        }

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName(name);
        definition.setDescription(data.get("description") != null ? String.valueOf(data.get("description")) : null);
        definition.setVersion(1);
        definition.setStatus(WorkflowDefinition.WorkflowStatus.DRAFT);
        definition.setNodes(nodes);
        definition.setEdges(edges);
        @SuppressWarnings("unchecked")
        Map<String, Object> triggers = data.get("triggers") instanceof Map ? (Map<String, Object>) data.get("triggers") : null;
        definition.setTriggers(triggers);
        definition.setTenantId(tenantId);

        WorkflowDefinition saved = workflowService.createDefinition(definition);
        saved.setBaseDefinitionId(saved.getId());
        saved = workflowService.updateDefinition(saved);

        return Result.success(WorkflowDefinitionVO.fromEntity(saved));
    }
}
