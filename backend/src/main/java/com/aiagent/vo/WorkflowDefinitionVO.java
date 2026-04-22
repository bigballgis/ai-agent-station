package com.aiagent.vo;

import com.aiagent.entity.WorkflowDefinition;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkflowDefinitionVO {
    private Long id;
    private String name;
    private String description;
    private Integer version;
    private String status;
    private Integer nodeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkflowDefinitionVO fromEntity(WorkflowDefinition entity) {
        WorkflowDefinitionVO vo = new WorkflowDefinitionVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setVersion(entity.getVersion());
        vo.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        vo.setNodeCount(entity.getNodes() != null ? entity.getNodes().size() : 0);
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
