package com.aiagent.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserVO extends BaseVO {
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String status;
    private Long tenantId;
    private String tenantName;
    private List<String> roles;
    // 脱敏字段
    private String maskedPhone;
    private String maskedEmail;
}
