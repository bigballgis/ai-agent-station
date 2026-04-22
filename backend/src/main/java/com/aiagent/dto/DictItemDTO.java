package com.aiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictItemDTO {
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签不能超过100个字符")
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Size(max = 100, message = "字典值不能超过100个字符")
    private String dictValue;

    private Integer dictSort;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String status;
}
