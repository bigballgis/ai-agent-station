package com.aiagent.controller;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.AlertRecord;
import com.aiagent.entity.AlertRule;
import com.aiagent.repository.AlertRecordRepository;
import com.aiagent.repository.AlertRuleRepository;
import com.aiagent.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleRepository ruleRepository;
    private final AlertRecordRepository recordRepository;

    // 告警规则 CRUD
    @GetMapping("/rules")
    public Result<List<AlertRule>> getRules() {
        return Result.success(ruleRepository.findByIsActiveTrue());
    }

    @PostMapping("/rules")
    public Result<AlertRule> createRule(@RequestBody AlertRule rule) {
        return Result.success(ruleRepository.save(rule));
    }

    @PutMapping("/rules/{id}")
    public Result<AlertRule> updateRule(@PathVariable Long id, @RequestBody AlertRule rule) {
        rule.setId(id);
        return Result.success(ruleRepository.save(rule));
    }

    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        ruleRepository.deleteById(id);
        return Result.success();
    }

    // 告警记录查询
    @GetMapping("/records")
    public Result<PageResult<AlertRecord>> getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "firedAt"));
        Page<AlertRecord> result = recordRepository.findByTenantIdOrderByFiredAtDesc(tenantId, pageable);
        return Result.success(PageResult.from(result));
    }

    @GetMapping("/records/active")
    public Result<List<AlertRecord>> getActiveAlerts() {
        return Result.success(recordRepository.findByStatus("firing"));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getAlertStats() {
        long activeCount = recordRepository.countByStatusAndFiredAtAfter("firing",
            LocalDateTime.now().minusHours(24));
        return Result.success(Map.of(
            "activeIn24h", activeCount,
            "totalRules", ruleRepository.count()
        ));
    }
}
