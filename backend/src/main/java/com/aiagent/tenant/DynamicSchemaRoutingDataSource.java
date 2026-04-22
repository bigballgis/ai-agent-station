package com.aiagent.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicSchemaRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(DynamicSchemaRoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        String schema = TenantContextHolder.getSchemaName();
        log.debug("当前使用的Schema: {}", schema);
        return schema;
    }
}
