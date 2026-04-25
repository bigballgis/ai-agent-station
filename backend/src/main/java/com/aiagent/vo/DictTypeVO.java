package com.aiagent.vo;

import com.aiagent.entity.DictType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeVO extends BaseVO {
    private String dictName;
    private String dictType;
    private String status;
    private Long itemCount;
    private String remark;

    public static DictTypeVO fromEntity(DictType entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(entity.getId());
        vo.setDictName(entity.getDictName());
        vo.setDictType(entity.getDictType());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
