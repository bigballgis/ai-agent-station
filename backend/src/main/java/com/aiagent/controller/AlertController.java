package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.AlertRecord;
import com.aiagent.entity.AlertRule;
import com.aiagent.repository.AlertRecordRepository;
import com.aiagent.repository.AlertRuleRepository;
import com.aiagent.util.SecurityUtils;
import com.aiagent.vo.AlertRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警管理接口")
public class AlertController {

    private final AlertRuleRepository ruleRepository;
    private final AlertRecordRepository recordRepository;

    // 告警规则 CRUD
    @RequiresPermission("alert:view")
    @GetMapping("/rules")
    @Operation(summary = "获取告警规则列表")
    public Result<List<AlertRuleVO>> getRules() {
        List<AlertRuleVO> voList = ruleRepository.findByIsActiveTrue().stream()
                .map(AlertRuleVO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    @RequiresPermission("alert:manage")
    @PostMapping("/rules")
    public Result<AlertRuleVO> createRule(@RequestBody AlertRule rule) {
        return Result.success(AlertRuleVO.fromEntity(ruleRepository.save(rule)));
    }

    @RequiresPermission("alert:manage")
    @PutMapping("/rules/{id}")
    public Result<AlertRuleVO> updateRule(@Parameter(description = "规则ID") @PathVariable Long id, @RequestBody AlertRule rule) {
        rule.setId(id);
        return Result.success(AlertRuleVO.fromEntity(ruleRepository.save(rule)));
    }

    @RequiresPermission("alert:manage")
    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@Parameter(description = "规则ID") @PathVariable Long id) {
        ruleRepository.deleteById(id);
        return Result.success();
    }

    // 告警记录查询
    @RequiresPermission("alert:view")
    @GetMapping("/records")
    @Operation(summary = "分页查询告警记录")
    public Result<PageResult<AlertRecord>> getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "firedAt"));
        Page<AlertRecord> result = recordRepository.findByTenantIdOrderByFiredAtDesc(tenantId, pageable);
        return Result.success(PageResult.from(result));
    }

    @RequiresPermission("alert:view")
    @GetMapping("/records/active")
    @Operation(summary = "获取活跃告警列表")
    public Result<List<AlertRecord>> getActiveAlerts() {
        return Result.success(recordRepository.findByStatus("firing"));
    }

    @RequiresPermission("alert:view")
    @GetMapping("/stats")
    @Operation(summary = "获取告警统计信息")
    public Result<Map<String, Object>> getAlertStats() {
        long activeCount = recordRepository.countByStatusAndFiredAtAfter("firing",
            LocalDateTime.now().minusHours(24));
        return Result.success(Map.of(
            "activeIn24h", activeCount,
            "totalRules", ruleRepository.count()
        ));
    }
}
