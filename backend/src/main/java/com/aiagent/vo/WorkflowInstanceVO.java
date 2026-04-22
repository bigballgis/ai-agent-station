package com.aiagent.vo;

import com.aiagent.entity.WorkflowInstance;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.Duration;

@Data
public class WorkflowInstanceVO {
    private Long id;
    private String workflowName;
    private String status;
    private String currentNodeId;
    private String currentNodeName;
    private Long startedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long duration;

    public static WorkflowInstanceVO fromEntity(WorkflowInstance entity) {
        WorkflowInstanceVO vo = new WorkflowInstanceVO();
        vo.setId(entity.getId());
        vo.setWorkflowName(entity.getWorkflowName());
        vo.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        vo.setCurrentNodeId(entity.getCurrentNodeId());
        vo.setStartedBy(entity.getStartedBy());
        vo.setStartedAt(entity.getStartedAt());
        vo.setCompletedAt(entity.getCompletedAt());
        if (entity.getStartedAt() != null && entity.getCompletedAt() != null) {
            vo.setDuration(Duration.between(entity.getStartedAt(), entity.getCompletedAt()).getSeconds());
        }
        return vo;
    }
}
