package com.aiagent.vo;

import com.aiagent.entity.Tenant;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantVO {
    private Long id;
    private String name;
    private String contactName;
    private String contactEmail;
    private String status;
    private Integer agentCount;
    private Integer userCount;
    private LocalDateTime createdAt;

    public static TenantVO fromEntity(Tenant entity) {
        TenantVO vo = new TenantVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setStatus(entity.getIsActive() != null && entity.getIsActive() ? "active" : "inactive");
        vo.setAgentCount(entity.getUsedAgents());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
