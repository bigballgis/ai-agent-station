package com.aiagent.controller;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.AgentApproval;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AgentApprovalService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class AgentApprovalController {

    private final AgentApprovalService agentApprovalService;

    @GetMapping
    public Result<PageResult<AgentApproval>> getApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AgentApproval> approvalPage = agentApprovalService.getApprovals(pageable);
        
        return Result.success(PageResult.from(approvalPage));
    }

    @GetMapping("/pending")
    public Result<PageResult<AgentApproval>> getPendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        Page<AgentApproval> approvalPage = agentApprovalService.getPendingApprovals(pageable);
        
        return Result.success(PageResult.from(approvalPage));
    }

    @GetMapping("/{id}")
    public Result<AgentApproval> getApprovalById(@PathVariable Long id) {
        AgentApproval approval = agentApprovalService.getApprovalById(id);
        return Result.success(approval);
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<AgentApproval>> getApprovalsByAgentId(@PathVariable Long agentId) {
        List<AgentApproval> approvals = agentApprovalService.getApprovalsByAgentId(agentId);
        return Result.success(approvals);
    }

    @PostMapping("/submit")
    public Result<AgentApproval> submitForApproval(
            @Valid @RequestBody SubmitApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        AgentApproval approval = agentApprovalService.submitForApproval(request.getAgentId(), request.getVersionId(), request.getRemark(), principal.getId());
        return Result.success(approval);
    }

    @PostMapping("/{id}/approve")
    public Result<AgentApproval> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        AgentApproval approval = agentApprovalService.approve(id, request.getApprovalRemark(), principal.getId());
        return Result.success(approval);
    }

    @PostMapping("/{id}/reject")
    public Result<AgentApproval> reject(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        AgentApproval approval = agentApprovalService.reject(id, request.getApprovalRemark(), principal.getId());
        return Result.success(approval);
    }

    @Data
    public static class SubmitApprovalRequest {
        private Long agentId;
        private Long versionId;
        private String remark;
    }

    @Data
    public static class ApprovalActionRequest {
        private String approvalRemark;
    }
}
