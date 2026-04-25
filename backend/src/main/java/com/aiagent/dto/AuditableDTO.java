package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 可审计 DTO 基类 - 包含审计字段 createdBy/updatedBy
 * 适用于需要追踪创建人/更新人的 DTO
 */
@Data
@Schema(description = "可审计DTO基类")
public class AuditableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "更新人ID")
    private Long updatedBy;
}
