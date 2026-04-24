package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Schema(description = "字典项数据传输对象")
public class DictItemDTO {

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型不能超过100个字符")
    @Schema(description = "字典类型", example = "agent_status", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签不能超过100个字符")
    @Schema(description = "字典标签", example = "活跃", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Size(max = 100, message = "字典值不能超过100个字符")
    @Schema(description = "字典值", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictValue;

    @Schema(description = "排序号", example = "1")
    private Integer dictSort;

    @Size(max = 100, message = "样式CSS类名不能超过100个字符")
    @Schema(description = "样式CSS类名")
    private String cssClass;

    @Size(max = 100, message = "表格回显样式不能超过100个字符")
    @Schema(description = "表格回显样式")
    private String listClass;

    @Size(max = 1, message = "是否默认不能超过1个字符")
    @Schema(description = "是否默认", example = "N")
    private String isDefault;

    @Size(max = 10, message = "状态不能超过10个字符")
    @Schema(description = "状态", example = "0")
    private String status;
}
