package com.aiagent.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TenantHibernatePropertiesCustomizer implements HibernatePropertiesCustomizer {

    private final TenantLineInterceptor tenantLineInterceptor;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.session_factory.statement_inspector", tenantLineInterceptor);
    }
}
