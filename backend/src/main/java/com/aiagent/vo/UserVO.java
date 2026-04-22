package com.aiagent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String status;
    private Long tenantId;
    private String tenantName;
    private List<String> roles;
    private LocalDateTime createdAt;
    // 脱敏字段
    private String maskedPhone;
    private String maskedEmail;
}
