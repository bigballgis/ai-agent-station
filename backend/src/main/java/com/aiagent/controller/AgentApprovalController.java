package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.ApprovalActionDTO;
import com.aiagent.dto.ApprovalSubmitDTO;
import com.aiagent.entity.AgentApproval;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AgentApprovalService;
import com.aiagent.security.validator.SortFieldValidator;
import com.aiagent.vo.AgentApprovalVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/approvals")
@RequiredArgsConstructor
@Tag(name = "审批管理", description = "审批管理接口")
public class AgentApprovalController {

    private final AgentApprovalService agentApprovalService;
    private final SortFieldValidator sortFieldValidator;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "submittedAt", "reviewedAt", "status", "agentName", "versionNumber");

    @RequiresPermission("approval:view")
    @GetMapping
    @Operation(summary = "分页查询审批列表")
    public Result<PageResult<AgentApprovalVO>> getApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String safeSortBy = sortFieldValidator.validate(sortBy, ALLOWED_SORT_FIELDS);
        String safeSortDir = sortFieldValidator.validateDirection(sortDir);
        int[] safePagination = sortFieldValidator.validatePagination(page, size, 100);

        Sort sort = safeSortDir.equalsIgnoreCase("desc") ? Sort.by(safeSortBy).descending() : Sort.by(safeSortBy).ascending();
        Pageable pageable = PageRequest.of(safePagination[0], safePagination[1], sort);
        Page<AgentApproval> approvalPage = agentApprovalService.getApprovals(pageable);
        Page<AgentApprovalVO> voPage = approvalPage.map(AgentApprovalVO::fromEntity);

        return Result.success(PageResult.from(voPage));
    }

    @RequiresPermission("approval:view")
    @GetMapping("/pending")
    @Operation(summary = "获取待审批列表")
    public Result<PageResult<AgentApprovalVO>> getPendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        Page<AgentApproval> approvalPage = agentApprovalService.getPendingApprovals(pageable);
        Page<AgentApprovalVO> voPage = approvalPage.map(AgentApprovalVO::fromEntity);

        return Result.success(PageResult.from(voPage));
    }

    @RequiresPermission("approval:view")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取审批详情")
    public Result<AgentApprovalVO> getApprovalById(@Parameter(description = "审批ID") @PathVariable Long id) {
        AgentApproval approval = agentApprovalService.getApprovalById(id);
        return Result.success(AgentApprovalVO.fromEntity(approval));
    }

    @RequiresPermission("approval:view")
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "根据Agent ID获取审批列表")
    public Result<List<AgentApprovalVO>> getApprovalsByAgentId(@Parameter(description = "Agent ID") @PathVariable Long agentId) {
        List<AgentApproval> approvals = agentApprovalService.getApprovalsByAgentId(agentId);
        return Result.success(approvals.stream().map(AgentApprovalVO::fromEntity).toList());
    }

    @RequiresPermission("approval:manage")
    @PostMapping("/submit")
    @Operation(summary = "提交审批申请")
    public Result<AgentApprovalVO> submitForApproval(
            @Valid @RequestBody ApprovalSubmitDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {

        AgentApproval approval = agentApprovalService.submitForApproval(request.getAgentId(), request.getVersionId(), request.getRemark(), principal.getId());
        return Result.success(AgentApprovalVO.fromEntity(approval));
    }

    @RequiresPermission("approval:manage")
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<AgentApprovalVO> approve(
            @Parameter(description = "审批ID") @PathVariable Long id,
            @Valid @RequestBody ApprovalActionDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {

        AgentApproval approval = agentApprovalService.approve(id, request.getApprovalRemark(), principal.getId());
        return Result.success(AgentApprovalVO.fromEntity(approval));
    }

    @RequiresPermission("approval:manage")
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<AgentApprovalVO> reject(
            @Parameter(description = "审批ID") @PathVariable Long id,
            @Valid @RequestBody ApprovalActionDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {

        AgentApproval approval = agentApprovalService.reject(id, request.getApprovalRemark(), principal.getId());
        return Result.success(AgentApprovalVO.fromEntity(approval));
    }

}
