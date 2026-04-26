package com.aiagent.service;

import com.aiagent.config.properties.AiAgentProperties;
import com.aiagent.dto.AlertRuleCreateDTO;
import com.aiagent.dto.AlertRuleUpdateDTO;
import com.aiagent.entity.AlertRecord;
import com.aiagent.entity.AlertRule;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AlertRecordRepository;
import com.aiagent.repository.AlertRuleRepository;
import com.aiagent.util.SecurityUtils;
import com.aiagent.vo.AlertRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository ruleRepository;
    private final AlertRecordRepository recordRepository;
    private final NotificationService notificationService;
    private final AiAgentProperties aiAgentProperties;

    private static final long[] WEBHOOK_BACKOFF_MS = {1000L, 5000L, 15000L};

    // ==================== Alert Rule ====================

    public List<AlertRuleVO> getActiveRules() {
        return ruleRepository.findByIsActiveTrue().stream()
                .map(AlertRuleVO::fromEntity)
                .collect(java.util.stream.Collectors.toList());
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
        rule.setWebhookUrl(dto.getWebhookUrl());
        return AlertRuleVO.fromEntity(ruleRepository.save(rule));
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertRuleVO updateRule(Long id, AlertRuleUpdateDTO dto) {
        AlertRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警规则不存在: " + id));
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
        if (dto.getWebhookUrl() != null) {
            rule.setWebhookUrl(dto.getWebhookUrl());
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
        return recordRepository.findByStatus("firing", PageRequest.of(0, 100)).getContent();
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

    // ==================== Alert Notification ====================

    /**
     * 触发告警通知，根据规则配置的通知渠道进行分发
     */
    public void fireAlert(AlertRule rule, AlertRecord record) {
        // Publish real-time ALERT_FIRED event via WebSocket
        try {
            notificationService.broadcastEvent(
                    com.aiagent.websocket.WebSocketEventDTO.alertFired(
                            rule.getName(),
                            record.getSeverity(),
                            record.getMessage()
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to publish alert fired event: {}", e.getMessage());
        }

        String channels = rule.getNotifyChannels();
        if (channels == null || channels.isBlank()) {
            channels = "email";
        }

        for (String channel : channels.split(",")) {
            String trimmed = channel.trim().toLowerCase();
            switch (trimmed) {
                case "webhook" -> sendWebhookNotification(rule, record);
                case "email" -> log.info("邮件通知暂未实现, 告警规则: {}, 记录: {}", rule.getName(), record.getId());
                case "inapp", "in-app" -> log.info("应用内通知暂未实现, 告警规则: {}, 记录: {}", rule.getName(), record.getId());
                default -> log.warn("未知的通知渠道: {}, 告警规则: {}", trimmed, rule.getName());
            }
        }
    }

    /**
     * 通过 Webhook 发送告警通知，支持重试（3次，指数退避）
     */
    private void sendWebhookNotification(AlertRule rule, AlertRecord record) {
        String webhookUrl = rule.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("告警规则 [{}] 配置了 webhook 渠道但未设置 webhookUrl", rule.getName());
            return;
        }

        Map<String, Object> payload = buildWebhookPayload(rule, record);
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        for (int attempt = 0; attempt < aiAgentProperties.getAlert().getWebhookMaxRetries(); attempt++) {
            try {
                if (attempt > 0) {
                    long backoff = WEBHOOK_BACKOFF_MS[attempt - 1];
                    log.info("Webhook 通知重试第 {} 次, 等待 {}ms, 告警规则: {}", attempt, backoff, rule.getName());
                    Thread.sleep(backoff);
                }

                ResponseEntity<String> response = restTemplate.exchange(
                        webhookUrl, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Webhook 通知发送成功, 告警规则: {}, HTTP状态: {}", rule.getName(), response.getStatusCode());
                    return;
                } else {
                    log.warn("Webhook 通知返回非成功状态码: {}, 告警规则: {}", response.getStatusCode(), rule.getName());
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Webhook 通知重试被中断, 告警规则: {}", rule.getName());
                return;
            } catch (Exception e) {
                log.warn("Webhook 通知发送失败 (尝试 {}/{}), 告警规则: {}, 错误: {}",
                        attempt + 1, aiAgentProperties.getAlert().getWebhookMaxRetries(), rule.getName(), e.getMessage());
            }
        }

        log.error("Webhook 通知最终发送失败, 已重试 {} 次, 告警规则: {}", aiAgentProperties.getAlert().getWebhookMaxRetries(), rule.getName());
    }

    /**
     * 构建 Webhook 通知的请求体
     */
    private Map<String, Object> buildWebhookPayload(AlertRule rule, AlertRecord record) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alertId", record.getId());
        payload.put("ruleId", rule.getId());
        payload.put("ruleName", rule.getName());
        payload.put("alertType", record.getAlertType());
        payload.put("severity", record.getSeverity());
        payload.put("status", record.getStatus());
        payload.put("message", record.getMessage());
        payload.put("metricValue", record.getMetricValue());
        payload.put("threshold", record.getThreshold());
        payload.put("firedAt", record.getFiredAt() != null ? record.getFiredAt().toString() : null);
        payload.put("tenantId", record.getTenantId());
        payload.put("timestamp", LocalDateTime.now().toString());
        return payload;
    }
}
