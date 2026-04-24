package com.aiagent.repository;

import com.aiagent.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByTenantIdAndIsActiveTrue(Long tenantId);

    @Deprecated
    List<AlertRule> findByIsActiveTrue();
}
