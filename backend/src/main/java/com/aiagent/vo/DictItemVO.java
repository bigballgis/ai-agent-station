package com.aiagent.vo;

import com.aiagent.entity.DictItem;
import lombok.Data;

@Data
public class DictItemVO {
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer dictSort;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String status;

    public static DictItemVO fromEntity(DictItem entity) {
        DictItemVO vo = new DictItemVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictLabel(entity.getDictLabel());
        vo.setDictValue(entity.getDictValue());
        vo.setDictSort(entity.getDictSort());
        vo.setCssClass(entity.getCssClass());
        vo.setListClass(entity.getListClass());
        vo.setIsDefault(entity.getIsDefault());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
