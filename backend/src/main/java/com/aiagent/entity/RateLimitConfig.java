package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "rate_limit_configs")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RateLimitConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "api_interface_id")
    private Long apiInterfaceId;

    @Column(name = "limit_type", nullable = false, length = 20)
    private String limitType = "GLOBAL";

    @Column(name = "requests_per_second", nullable = false)
    private Integer requestsPerSecond = 10;

    @Column(name = "requests_per_minute", nullable = false)
    private Integer requestsPerMinute = 100;

    @Column(name = "requests_per_hour", nullable = false)
    private Integer requestsPerHour = 1000;

    @Column(name = "requests_per_day", nullable = false)
    private Integer requestsPerDay = 10000;

    @Column(name = "burst_capacity", nullable = false)
    private Integer burstCapacity = 20;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", insertable = false, updatable = false)
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_interface_id", insertable = false, updatable = false)
    private ApiInterface apiInterface;
}
