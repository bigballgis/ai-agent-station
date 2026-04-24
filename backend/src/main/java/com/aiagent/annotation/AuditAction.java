package com.aiagent.annotation;

/**
 * 审计操作类型枚举
 */
public enum AuditAction {
    /** 登录 */
    LOGIN,
    /** 登出 */
    LOGOUT,
    /** 创建资源 */
    CREATE,
    /** 更新资源 */
    UPDATE,
    /** 删除资源 */
    DELETE,
    /** 数据导出 */
    EXPORT,
    /** 数据导入 */
    IMPORT,
    /** 审批通过 */
    APPROVE,
    /** 审批拒绝 */
    REJECT,
    /** 密码修改/重置 */
    PASSWORD_CHANGE,
    /** 部署操作 */
    DEPLOY,
    /** 其他操作 */
    OTHER
}
