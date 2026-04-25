package com.aiagent.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemLogVO extends BaseVO {

    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String params;
    private String ip;
    private String userAgent;
    private Long executionTime;
    private Boolean isSuccess;
    private String errorMsg;

    public static SystemLogVO fromEntity(com.aiagent.entity.SystemLog entity) {
        SystemLogVO vo = new SystemLogVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setModule(entity.getModule());
        vo.setOperation(entity.getOperation());
        vo.setMethod(entity.getMethod());
        vo.setParams(entity.getParams());
        vo.setIp(entity.getIp());
        vo.setUserAgent(entity.getUserAgent());
        vo.setExecutionTime(entity.getExecutionTime());
        vo.setIsSuccess(entity.getIsSuccess());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
