package com.aiagent.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 更新经验请求 DTO
 */
@Data
public class UpdateExperienceRequestDTO {

    @Size(max = 100, message = "经验标题不能超过100个字符")
    private String title;

    private String description;

    private String content;

    private List<String> tags;

    private BigDecimal effectivenessScore;

    private Integer status;

    private Long updatedBy;
}
