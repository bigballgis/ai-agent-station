package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "字典类型数据传输对象")
public class DictTypeDTO {

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称不能超过100个字符")
    @Schema(description = "字典名称", example = "Agent状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型不能超过100个字符")
    @Schema(description = "字典类型", example = "agent_status", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictType;

    @Size(max = 10, message = "状态不能超过10个字符")
    @Schema(description = "状态", example = "0")
    private String status;

    @Size(max = 200, message = "备注不能超过200个字符")
    @Schema(description = "备注")
    private String remark;
}
