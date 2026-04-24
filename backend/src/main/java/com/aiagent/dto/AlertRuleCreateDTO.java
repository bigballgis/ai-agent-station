package com.aiagent.dto;

import com.aiagent.entity.AlertRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建告警规则请求")
public class AlertRuleCreateDTO {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称不能超过100个字符")
    @Schema(description = "规则名称", example = "API错误率告警", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    @Schema(description = "规则描述", example = "当API错误率超过阈值时触发告警")
    private String description;

    @NotNull(message = "告警类型不能为空")
    @Schema(description = "告警类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private AlertRule.AlertType alertType;

    @NotBlank(message = "指标名称不能为空")
    @Schema(description = "指标名称", example = "api.error.rate", requiredMode = Schema.RequiredMode.REQUIRED)
    private String metricName;

    @Schema(description = "阈值")
    private Double threshold;

    @Schema(description = "比较运算符", example = "gt")
    private String comparisonOperator = "gt";

    @Schema(description = "持续时间(秒)", example = "300")
    private Integer durationSeconds = 300;

    @Schema(description = "严重程度", example = "WARNING")
    private AlertRule.Severity severity = AlertRule.Severity.WARNING;

    @Schema(description = "是否启用", example = "true")
    private Boolean isActive = true;

    @Schema(description = "通知渠道", example = "email")
    private String notifyChannels = "email";

    @Schema(description = "通知目标")
    private String notifyTargets;

    @Schema(description = "Webhook回调地址", example = "https://example.com/webhook/alert")
    private String webhookUrl;
}
