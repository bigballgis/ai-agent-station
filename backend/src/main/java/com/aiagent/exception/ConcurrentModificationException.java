package com.aiagent.exception;

import com.aiagent.common.ResultCode;

/**
 * 并发修改异常 (409)
 *
 * 用于乐观锁失败、数据版本冲突等并发修改场景。
 * 包含实体类型和ID等上下文信息，便于排查问题。
 */
public class ConcurrentModificationException extends BusinessException {

    /** 发生冲突的实体类型 */
    private final String entityType;

    /** 发生冲突的实体ID */
    private final String entityId;

    public ConcurrentModificationException(String message) {
        super(ResultCode.CONCURRENT_MODIFICATION.getCode(), ResultCode.CONCURRENT_MODIFICATION.getMessageCode(), message);
        this.entityType = null;
        this.entityId = null;
    }

    public ConcurrentModificationException(String entityType, String entityId) {
        super(ResultCode.CONCURRENT_MODIFICATION.getCode(),
                ResultCode.CONCURRENT_MODIFICATION.getMessageCode(),
                "数据已被其他用户修改，请刷新后重试");
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public ConcurrentModificationException(String entityType, String entityId, String message) {
        super(ResultCode.CONCURRENT_MODIFICATION.getCode(), ResultCode.CONCURRENT_MODIFICATION.getMessageCode(), message);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public ConcurrentModificationException() {
        super(ResultCode.CONCURRENT_MODIFICATION);
        this.entityType = null;
        this.entityId = null;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
