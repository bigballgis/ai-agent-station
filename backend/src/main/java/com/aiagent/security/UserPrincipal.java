package com.aiagent.security;

import java.io.Serializable;

public class UserPrincipal implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private String username;
    private Long tenantId;

    public UserPrincipal() {
    }

    public UserPrincipal(Long userId, String username, Long tenantId) {
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getId() {
        return userId;
    }
}
