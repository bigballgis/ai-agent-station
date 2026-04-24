package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建经验请求 DTO
 */
@Data
public class CreateExperienceRequestDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    @NotBlank(message = "经验类型不能为空")
    @Size(max = 50, message = "经验类型不能超过50个字符")
    private String experienceType;

    @NotBlank(message = "经验编码不能为空")
    @Size(max = 50, message = "经验编码不能超过50个字符")
    private String experienceCode;

    @NotBlank(message = "经验标题不能为空")
    @Size(max = 100, message = "经验标题不能超过100个字符")
    private String title;

    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;

    @Size(max = 10000, message = "经验内容不能超过10000个字符")
    private String content;

    private List<String> tags;

    @DecimalMin(value = "0.0", message = "有效性评分不能为负数")
    @DecimalMax(value = "10.0", message = "有效性评分不能超过10")
    private BigDecimal effectivenessScore;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不能为负数")
    private Integer status;

    @NotNull(message = "创建人ID不能为空")
    private Long createdBy;
}
