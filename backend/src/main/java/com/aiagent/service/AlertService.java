package com.aiagent.service;

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
    public AlertRuleVO createRule(AlertRule rule) {
        return AlertRuleVO.fromEntity(ruleRepository.save(rule));
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertRuleVO updateRule(Long id, AlertRule rule) {
        rule.setId(id);
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
