package com.aiagent.vo;

import com.aiagent.entity.DeploymentHistory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeploymentVO {
    private Long id;
    private String agentName;
    private Long agentId;
    private String environment;
    private String status;
    private String version;
    private Long deployedBy;
    private LocalDateTime deployedAt;

    public static DeploymentVO fromEntity(DeploymentHistory entity) {
        DeploymentVO vo = new DeploymentVO();
        vo.setId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        vo.setVersion(entity.getVersion());
        vo.setDeployedBy(entity.getDeployerId());
        vo.setDeployedAt(entity.getDeployedAt());
        if (entity.getAgent() != null) {
            vo.setAgentName(entity.getAgent().getName());
        }
        return vo;
    }
}
