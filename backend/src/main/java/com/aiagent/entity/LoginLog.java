package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "login_logs")
public class LoginLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_type", nullable = false, length = 20)
    private String loginType; // LOGIN, LOGOUT, LOGIN_FAIL

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAIL

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(name = "tenant_id")
    private Long tenantId;

    @PrePersist
    protected void onCreate() {
        if (this.loginTime == null) {
            this.loginTime = LocalDateTime.now();
        }
    }
}
