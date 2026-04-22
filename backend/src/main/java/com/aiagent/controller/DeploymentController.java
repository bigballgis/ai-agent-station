package com.aiagent.controller;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.DeploymentHistory;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.DeploymentService;
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
import java.util.Map;

@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping
    public Result<PageResult<DeploymentHistory>> getDeploymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DeploymentHistory> deploymentPage = deploymentService.getDeploymentHistory(pageable);
        
        return Result.success(PageResult.from(deploymentPage));
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<DeploymentHistory>> getDeploymentHistoryByAgentId(@PathVariable Long agentId) {
        List<DeploymentHistory> deployments = deploymentService.getDeploymentHistoryByAgentId(agentId);
        return Result.success(deployments);
    }

    @GetMapping("/{id}")
    public Result<DeploymentHistory> getDeploymentById(@PathVariable Long id) {
        DeploymentHistory deployment = deploymentService.getDeploymentById(id);
        return Result.success(deployment);
    }

    @PostMapping("/deploy")
    public Result<DeploymentHistory> deploy(
            @Valid @RequestBody DeployRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        DeploymentHistory deployment = deploymentService.deploy(
                request.getAgentId(), request.getVersionId(), 
                request.getIsCanary(), request.getCanaryPercentage(), 
                request.getRemark(), principal.getId());
        return Result.success(deployment);
    }

    @PostMapping("/{id}/rollback")
    public Result<DeploymentHistory> rollback(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        DeploymentHistory deployment = deploymentService.rollback(id, principal.getId());
        return Result.success(deployment);
    }

    @GetMapping("/compare")
    public Result<Map<String, Object>> compareVersions(
            @RequestParam Long versionId1,
            @RequestParam Long versionId2) {
        
        Map<String, Object> comparison = deploymentService.compareVersions(versionId1, versionId2);
        return Result.success(comparison);
    }

    @Data
    public static class DeployRequest {
        private Long agentId;
        private Long versionId;
        private Boolean isCanary = false;
        private Integer canaryPercentage = 100;
        private String remark;
    }
}
