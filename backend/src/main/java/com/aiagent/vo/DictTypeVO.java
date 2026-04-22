package com.aiagent.vo;

import com.aiagent.entity.DictType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictTypeVO {
    private Long id;
    private String dictName;
    private String dictType;
    private String status;
    private Long itemCount;
    private String remark;
    private LocalDateTime createdAt;

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
