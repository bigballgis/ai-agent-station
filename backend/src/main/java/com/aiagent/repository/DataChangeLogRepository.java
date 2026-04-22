package com.aiagent.repository;

import com.aiagent.entity.DataChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DataChangeLogRepository extends JpaRepository<DataChangeLog, Long>, JpaSpecificationExecutor<DataChangeLog> {
    List<DataChangeLog> findByTableNameAndRecordIdOrderByOperatedAtDesc(String tableName, String recordId);

    List<DataChangeLog> findByOperatorOrderByOperatedAtDesc(String operator);
}
