package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.AlertRecord;
import com.aiagent.entity.AlertRule;
import com.aiagent.service.AlertService;
import com.aiagent.vo.AlertRecordVO;
import com.aiagent.vo.AlertRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警管理接口")
public class AlertController {

    private final AlertService alertService;

    // 告警规则 CRUD
    @RequiresPermission("alert:view")
    @GetMapping("/rules")
    @Operation(summary = "获取告警规则列表")
    public Result<List<AlertRuleVO>> getRules() {
        return Result.success(alertService.getActiveRules());
    }

    @RequiresPermission("alert:manage")
    @PostMapping("/rules")
    @Operation(summary = "创建告警规则")
    public Result<AlertRuleVO> createRule(@Valid @RequestBody AlertRule rule) {
        return Result.success(alertService.createRule(rule));
    }

    @RequiresPermission("alert:manage")
    @PutMapping("/rules/{id}")
    @Operation(summary = "更新告警规则")
    public Result<AlertRuleVO> updateRule(@Parameter(description = "规则ID") @PathVariable Long id, @Valid @RequestBody AlertRule rule) {
        return Result.success(alertService.updateRule(id, rule));
    }

    @RequiresPermission("alert:manage")
    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除告警规则")
    public Result<Void> deleteRule(@Parameter(description = "规则ID") @PathVariable Long id) {
        alertService.deleteRule(id);
        return Result.success();
    }

    // 告警记录查询
    @RequiresPermission("alert:view")
    @GetMapping("/records")
    @Operation(summary = "分页查询告警记录")
    public Result<PageResult<AlertRecordVO>> getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "firedAt"));
        Page<AlertRecord> result = alertService.getRecords(page, size, pageable);
        Page<AlertRecordVO> voPage = result.map(AlertRecordVO::fromEntity);
        return Result.success(PageResult.from(voPage));
    }

    @RequiresPermission("alert:view")
    @GetMapping("/records/active")
    @Operation(summary = "获取活跃告警列表")
    public Result<List<AlertRecordVO>> getActiveAlerts() {
        return Result.success(alertService.getActiveAlerts().stream()
                .map(AlertRecordVO::fromEntity).toList());
    }

    @RequiresPermission("alert:view")
    @GetMapping("/stats")
    @Operation(summary = "获取告警统计信息")
    public Result<Map<String, Object>> getAlertStats() {
        return Result.success(alertService.getAlertStats());
    }

    @RequiresPermission("alert:manage")
    @PostMapping("/{id}/resolve")
    @Operation(summary = "解决告警记录")
    public Result<Void> resolveAlertRecord(@PathVariable Long id) {
        alertService.resolveAlertRecord(id);
        return Result.success(null);
    }
}
