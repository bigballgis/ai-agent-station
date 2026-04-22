package com.aiagent.vo;

import com.aiagent.entity.DataChangeLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataChangeLogVO {
    private Long id;
    private String tableName;
    private String recordId;
    private String operationType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String operator;
    private LocalDateTime operatedAt;

    public static DataChangeLogVO fromEntity(DataChangeLog entity) {
        DataChangeLogVO vo = new DataChangeLogVO();
        vo.setId(entity.getId());
        vo.setTableName(entity.getTableName());
        vo.setRecordId(entity.getRecordId());
        vo.setOperationType(entity.getOperationType());
        vo.setFieldName(entity.getFieldName());
        vo.setOldValue(entity.getOldValue());
        vo.setNewValue(entity.getNewValue());
        vo.setOperator(entity.getOperator());
        vo.setOperatedAt(entity.getOperatedAt());
        return vo;
    }
}
