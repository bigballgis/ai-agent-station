package com.aiagent.vo;

import com.aiagent.entity.SystemLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "系统日志视图对象")
public class SystemLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "模块")
    private String module;

    @Schema(description = "操作")
    private String operation;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求参数")
    private String params;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "执行时间(ms)")
    private Long executionTime;

    @Schema(description = "是否成功")
    private Boolean isSuccess;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public static SystemLogVO fromEntity(SystemLog entity) {
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
