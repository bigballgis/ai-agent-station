package com.aiagent.vo;

import com.aiagent.entity.AlertRule;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertRuleVO {
    private Long id;
    private String name;
    private String severity;
    private String status;
    private Boolean enabled;
    private String notifyChannels;
    private String webhookUrl;
    private LocalDateTime createdAt;

    public static AlertRuleVO fromEntity(AlertRule entity) {
        AlertRuleVO vo = new AlertRuleVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setSeverity(entity.getSeverity() != null ? entity.getSeverity().name() : null);
        vo.setStatus("active");
        vo.setEnabled(entity.getIsActive());
        vo.setNotifyChannels(entity.getNotifyChannels());
        vo.setWebhookUrl(entity.getWebhookUrl());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
