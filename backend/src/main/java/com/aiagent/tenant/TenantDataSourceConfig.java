package com.aiagent.tenant;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class TenantDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${ai-agent.tenant.default-schema:public}")
    private String defaultSchema;

    @Bean
    @Primary
    public DataSource dynamicDataSource() {
        DynamicSchemaRoutingDataSource routingDataSource = new DynamicSchemaRoutingDataSource();

        DataSource defaultDataSource = createDataSource(defaultSchema);
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(defaultSchema, defaultDataSource);

        routingDataSource.setDefaultTargetDataSource(defaultDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }

    private DataSource createDataSource(String schema) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url.replace("?", "?currentSchema=" + schema + "&"));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);
        return dataSource;
    }
}
