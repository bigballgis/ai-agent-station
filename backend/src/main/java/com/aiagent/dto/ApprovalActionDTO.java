package com.aiagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "审批操作请求")
public class ApprovalActionDTO {

    @Size(max = 500, message = "审批备注不能超过500个字符")
    @Schema(description = "审批备注", example = "审核通过，可以发布")
    private String approvalRemark;
}
