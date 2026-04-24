package com.aiagent.repository;

import com.aiagent.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByTenantIdAndIsActiveTrue(Long tenantId);

    List<AlertRule> findByTenantId(Long tenantId);

    @Deprecated
    List<AlertRule> findByIsActiveTrue();
}
