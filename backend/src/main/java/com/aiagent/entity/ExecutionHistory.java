package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "execution_history")
public class ExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "role", length = 20)
    private String role;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
