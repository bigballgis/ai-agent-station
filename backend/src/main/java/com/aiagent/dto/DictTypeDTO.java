package com.aiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictTypeDTO {
    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称不能超过100个字符")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型不能超过100个字符")
    private String dictType;

    private String status;
    private String remark;
}
