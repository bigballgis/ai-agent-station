package com.aiagent.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

@Component
public class TenantLineInterceptor implements StatementInspector {

    private static final Logger log = LoggerFactory.getLogger(TenantLineInterceptor.class);

    @Override
    public String inspect(String sql) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && tenantId > 0 && !isSchemaManagementQuery(sql)) {
            String modifiedSql = addTenantCondition(sql, tenantId);
            log.debug("SQL添加租户过滤: {}", modifiedSql);
            return modifiedSql;
        }
        return sql;
    }

    private String addTenantCondition(String sql, Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return sql; // 无效租户ID，不添加过滤
        }
        // 防御性类型验证：确保 tenantId 是有效数字，防止潜在的注入风险
        try {
            long validated = Long.parseLong(String.valueOf(tenantId));
            if (validated <= 0) {
                return sql;
            }
        } catch (NumberFormatException e) {
            log.warn("无效的租户ID，跳过租户过滤: {}", tenantId);
            return sql;
        }
        String lowerSql = sql.toLowerCase();
        
        if (lowerSql.startsWith("select")) {
            return addWhereCondition(sql, tenantId, "WHERE", "AND");
        } else if (lowerSql.startsWith("insert")) {
            return sql;
        } else if (lowerSql.startsWith("update") || lowerSql.startsWith("delete")) {
            return addWhereCondition(sql, tenantId, "WHERE", "AND");
        }
        return sql;
    }

    private String addWhereCondition(String sql, Long tenantId, String whereKeyword, String andKeyword) {
        String lowerSql = sql.toLowerCase();
        int whereIndex = lowerSql.indexOf(" where ");
        String tenantCondition = " tenant_id = " + tenantId;
        
        if (whereIndex != -1) {
            return sql.substring(0, whereIndex + 7) + tenantCondition + " " + andKeyword + sql.substring(whereIndex + 7);
        } else {
            int fromIndex = lowerSql.indexOf(" from ");
            if (fromIndex != -1) {
                int endIndex = findEndOfFromClause(sql, fromIndex + 6);
                return sql.substring(0, endIndex) + " " + whereKeyword + tenantCondition + sql.substring(endIndex);
            }
        }
        return sql;
    }

    private int findEndOfFromClause(String sql, int startIndex) {
        int index = startIndex;
        int parenCount = 0;
        while (index < sql.length()) {
            char c = sql.charAt(index);
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
            if (parenCount == 0 && (c == ' ' || c == ';' || c == ',' || c == 'g' || c == 'o' || c == 'r')) {
                String sub = sql.substring(Math.max(0, index - 5), index + 1).toLowerCase();
                if (sub.contains(" group ") || sub.contains(" order ") || sub.contains(" limit ") || 
                    sub.contains(";") || c == ',') {
                    return index;
                }
            }
            index++;
        }
        return sql.length();
    }

    private boolean isSchemaManagementQuery(String sql) {
        String lowerSql = sql.toLowerCase();
        return lowerSql.startsWith("create") || lowerSql.startsWith("alter") || 
               lowerSql.startsWith("drop") || lowerSql.startsWith("truncate") ||
               lowerSql.startsWith("comment") || lowerSql.startsWith("insert into flyway");
    }
}
