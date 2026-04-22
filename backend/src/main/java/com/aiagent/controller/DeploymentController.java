package com.aiagent.controller;

import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.DeploymentHistory;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.DeploymentService;
import com.aiagent.vo.DeploymentVO;
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
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/deployments")
@RequiredArgsConstructor
@Tag(name = "部署管理", description = "部署管理接口")
public class DeploymentController {

    private final DeploymentService deploymentService;

    @RequiresPermission("deployment:view")
    @GetMapping
    @Operation(summary = "分页查询部署历史")
    public Result<PageResult<DeploymentVO>> getDeploymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DeploymentHistory> deploymentPage = deploymentService.getDeploymentHistory(pageable);

        List<DeploymentVO> voList = deploymentPage.getContent().stream()
                .map(DeploymentVO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(new PageResult<>(deploymentPage.getTotalElements(), voList));
    }

    @RequiresPermission("deployment:view")
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "根据Agent ID获取部署历史")
    public Result<List<DeploymentVO>> getDeploymentHistoryByAgentId(@PathVariable Long agentId) {
        List<DeploymentVO> voList = deploymentService.getDeploymentHistoryByAgentId(agentId).stream()
                .map(DeploymentVO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    @RequiresPermission("deployment:view")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取部署详情")
    public Result<DeploymentVO> getDeploymentById(@PathVariable Long id) {
        DeploymentHistory deployment = deploymentService.getDeploymentById(id);
        return Result.success(DeploymentVO.fromEntity(deployment));
    }

    @RequiresPermission("deployment:manage")
    @PostMapping("/deploy")
    @Operation(summary = "部署Agent")
    @OperationLog(value = "部署Agent", module = "部署管理")
    public Result<DeploymentVO> deploy(
            @Valid @RequestBody DeployRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeploymentHistory deployment = deploymentService.deploy(
                request.getAgentId(), request.getVersionId(),
                request.getIsCanary(), request.getCanaryPercentage(),
                request.getRemark(), principal.getId());
        return Result.success(DeploymentVO.fromEntity(deployment));
    }

    @RequiresPermission("deployment:manage")
    @PostMapping("/{id}/rollback")
    @Operation(summary = "回滚部署")
    @OperationLog(value = "回滚部署", module = "部署管理")
    public Result<DeploymentVO> rollback(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeploymentHistory deployment = deploymentService.rollback(id, principal.getId());
        return Result.success(DeploymentVO.fromEntity(deployment));
    }

    @RequiresPermission("deployment:view")
    @GetMapping("/compare")
    @Operation(summary = "比较两个版本差异")
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
