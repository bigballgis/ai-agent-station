package com.aiagent.vo;

import com.aiagent.entity.AlertRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "告警记录视图对象")
public class AlertRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "告警类型")
    private String alertType;

    @Schema(description = "严重程度")
    private String severity;

    @Schema(description = "告警消息")
    private String message;

    @Schema(description = "指标值")
    private Double metricValue;

    @Schema(description = "阈值")
    private Double threshold;

    @Schema(description = "告警状态")
    private String status;

    @Schema(description = "触发时间")
    private LocalDateTime firedAt;

    @Schema(description = "解决时间")
    private LocalDateTime resolvedAt;

    public static AlertRecordVO fromEntity(AlertRecord entity) {
        AlertRecordVO vo = new AlertRecordVO();
        vo.setId(entity.getId());
        vo.setRuleId(entity.getRuleId());
        vo.setRuleName(entity.getRuleName());
        vo.setAlertType(entity.getAlertType());
        vo.setSeverity(entity.getSeverity());
        vo.setMessage(entity.getMessage());
        vo.setMetricValue(entity.getMetricValue());
        vo.setThreshold(entity.getThreshold());
        vo.setStatus(entity.getStatus());
        vo.setFiredAt(entity.getFiredAt());
        vo.setResolvedAt(entity.getResolvedAt());
        return vo;
    }
}
