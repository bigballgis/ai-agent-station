package com.aiagent.vo;

import com.aiagent.entity.ApiInterface;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "API接口视图对象")
public class ApiInterfaceVO extends BaseVO {

    @Schema(description = "关联Agent ID")
    private Long agentId;

    @Schema(description = "关联版本ID")
    private Long versionId;

    @Schema(description = "接口路径")
    private String path;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "接口描述")
    private String description;

    @Schema(description = "是否启用")
    private Boolean isActive;

    @Schema(description = "API版本号")
    private String apiVersion;

    @Schema(description = "是否已废弃")
    private Boolean deprecated;

    @Schema(description = "废弃说明")
    private String deprecationMessage;

    @Schema(description = "基础API ID（版本链路）")
    private Long baseApiId;

    public static ApiInterfaceVO fromEntity(ApiInterface entity) {
        ApiInterfaceVO vo = new ApiInterfaceVO();
        vo.setId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setVersionId(entity.getVersionId());
        vo.setPath(entity.getPath());
        vo.setMethod(entity.getMethod());
        vo.setDescription(entity.getDescription());
        vo.setIsActive(entity.getIsActive());
        vo.setApiVersion(entity.getApiVersion());
        vo.setDeprecated(entity.getDeprecated());
        vo.setDeprecationMessage(entity.getDeprecationMessage());
        vo.setBaseApiId(entity.getBaseApiId());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
