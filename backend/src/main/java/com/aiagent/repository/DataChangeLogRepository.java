package com.aiagent.repository;

import com.aiagent.entity.DataChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface DataChangeLogRepository extends JpaRepository<DataChangeLog, Long>, JpaSpecificationExecutor<DataChangeLog> {
    List<DataChangeLog> findByTableNameAndRecordIdOrderByOperatedAtDesc(String tableName, String recordId);

    List<DataChangeLog> findByOperatorOrderByOperatedAtDesc(String operator);
}
