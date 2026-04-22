package com.aiagent.vo;

import com.aiagent.entity.LoginLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogVO {
    private Long id;
    private String username;
    private String loginType;
    private String ip;
    private String browser;
    private String os;
    private String status;
    private String message;
    private LocalDateTime loginTime;

    public static LoginLogVO fromEntity(LoginLog entity) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setLoginType(entity.getLoginType());
        vo.setIp(entity.getIp());
        vo.setBrowser(entity.getBrowser());
        vo.setOs(entity.getOs());
        vo.setStatus(entity.getStatus());
        vo.setMessage(entity.getMessage());
        vo.setLoginTime(entity.getLoginTime());
        return vo;
    }
}
