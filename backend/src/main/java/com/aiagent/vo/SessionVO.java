package com.aiagent.vo;

import com.aiagent.entity.UserSession;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class SessionVO extends BaseVO {
    private String username;
    private String ip;
    private String browser;
    private String os;
    private LocalDateTime loginTime;
    private LocalDateTime lastAccessTime;
    private String status;

    public static SessionVO fromEntity(UserSession entity) {
        SessionVO vo = new SessionVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setIp(entity.getIpAddress());
        vo.setBrowser(entity.getBrowser());
        vo.setOs(entity.getOs());
        vo.setLoginTime(entity.getLoginTime());
        vo.setLastAccessTime(entity.getLastAccessTime());
        vo.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        return vo;
    }
}
