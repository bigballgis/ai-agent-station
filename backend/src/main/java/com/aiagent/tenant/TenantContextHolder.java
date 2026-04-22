package com.aiagent.tenant;

public class TenantContextHolder {
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> SCHEMA_NAME_HOLDER = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    public static void setSchemaName(String schemaName) {
        SCHEMA_NAME_HOLDER.set(schemaName);
    }

    public static String getSchemaName() {
        return SCHEMA_NAME_HOLDER.get();
    }

    public static void clear() {
        TENANT_ID_HOLDER.remove();
        SCHEMA_NAME_HOLDER.remove();
    }
}
