package com.aiagent.service;

import com.aiagent.dto.AlertRuleCreateDTO;
import com.aiagent.dto.AlertRuleUpdateDTO;
import com.aiagent.entity.AlertRecord;
import com.aiagent.entity.AlertRule;
import com.aiagent.repository.AlertRecordRepository;
import com.aiagent.repository.AlertRuleRepository;
import com.aiagent.util.SecurityUtils;
import com.aiagent.vo.AlertRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository ruleRepository;
    private final AlertRecordRepository recordRepository;

    // ==================== Alert Rule ====================

    public List<AlertRuleVO> getActiveRules() {
        return ruleRepository.findByIsActiveTrue().stream()
                .map(AlertRuleVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertRuleVO createRule(AlertRuleCreateDTO dto) {
        AlertRule rule = new AlertRule();
        rule.setName(dto.getName());
        rule.setDescription(dto.getDescription());
        rule.setAlertType(dto.getAlertType());
        rule.setMetricName(dto.getMetricName());
        rule.setThreshold(dto.getThreshold());
        rule.setComparisonOperator(dto.getComparisonOperator() != null ? dto.getComparisonOperator() : "gt");
        rule.setDurationSeconds(dto.getDurationSeconds() != null ? dto.getDurationSeconds() : 300);
        rule.setSeverity(dto.getSeverity() != null ? dto.getSeverity() : AlertRule.Severity.WARNING);
        rule.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        rule.setNotifyChannels(dto.getNotifyChannels() != null ? dto.getNotifyChannels() : "email");
        rule.setNotifyTargets(dto.getNotifyTargets());
        return AlertRuleVO.fromEntity(ruleRepository.save(rule));
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertRuleVO updateRule(Long id, AlertRuleUpdateDTO dto) {
        AlertRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("告警规则不存在: " + id));
        if (dto.getName() != null) {
            rule.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            rule.setDescription(dto.getDescription());
        }
        if (dto.getAlertType() != null) {
            rule.setAlertType(dto.getAlertType());
        }
        if (dto.getMetricName() != null) {
            rule.setMetricName(dto.getMetricName());
        }
        if (dto.getThreshold() != null) {
            rule.setThreshold(dto.getThreshold());
        }
        if (dto.getComparisonOperator() != null) {
            rule.setComparisonOperator(dto.getComparisonOperator());
        }
        if (dto.getDurationSeconds() != null) {
            rule.setDurationSeconds(dto.getDurationSeconds());
        }
        if (dto.getSeverity() != null) {
            rule.setSeverity(dto.getSeverity());
        }
        if (dto.getIsActive() != null) {
            rule.setIsActive(dto.getIsActive());
        }
        if (dto.getNotifyChannels() != null) {
            rule.setNotifyChannels(dto.getNotifyChannels());
        }
        if (dto.getNotifyTargets() != null) {
            rule.setNotifyTargets(dto.getNotifyTargets());
        }
        return AlertRuleVO.fromEntity(ruleRepository.save(rule));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    // ==================== Alert Record ====================

    public Page<AlertRecord> getRecords(int page, int size, Pageable pageable) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return recordRepository.findByTenantIdOrderByFiredAtDesc(tenantId, pageable);
    }

    public List<AlertRecord> getActiveAlerts() {
        return recordRepository.findByStatus("firing");
    }

    public Map<String, Object> getAlertStats() {
        long activeCount = recordRepository.countByStatusAndFiredAtAfter("firing",
                LocalDateTime.now().minusHours(24));
        return Map.of(
                "activeIn24h", activeCount,
                "totalRules", ruleRepository.count()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolveAlertRecord(Long id) {
        recordRepository.findById(id).ifPresent(record -> {
            record.setStatus("RESOLVED");
            record.setResolvedAt(LocalDateTime.now());
            recordRepository.save(record);
        });
    }
}
