package com.aiagent.vo;

import com.aiagent.entity.AgentApproval;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "审批记录视图对象")
public class AgentApprovalVO {

    @Schema(description = "审批ID")
    private Long id;

    @Schema(description = "Agent ID")
    private Long agentId;

    @Schema(description = "Agent版本ID")
    private Long agentVersionId;

    @Schema(description = "提交人ID")
    private Long submitterId;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批状态")
    private String status;

    @Schema(description = "提交备注")
    private String remark;

    @Schema(description = "审批备注")
    private String approvalRemark;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "审批时间")
    private LocalDateTime approvedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public static AgentApprovalVO fromEntity(AgentApproval entity) {
        AgentApprovalVO vo = new AgentApprovalVO();
        vo.setId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setAgentVersionId(entity.getAgentVersionId());
        vo.setSubmitterId(entity.getSubmitterId());
        vo.setApproverId(entity.getApproverId());
        vo.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        vo.setRemark(entity.getRemark());
        vo.setApprovalRemark(entity.getApprovalRemark());
        vo.setSubmittedAt(entity.getSubmittedAt());
        vo.setApprovedAt(entity.getApprovedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
