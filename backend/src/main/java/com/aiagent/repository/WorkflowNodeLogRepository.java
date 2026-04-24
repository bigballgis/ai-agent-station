package com.aiagent.repository;

import com.aiagent.entity.WorkflowNodeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface WorkflowNodeLogRepository extends JpaRepository<WorkflowNodeLog, Long> {

    List<WorkflowNodeLog> findByInstanceIdOrderByStartedAtAsc(Long instanceId);

    List<WorkflowNodeLog> findByInstanceIdAndNodeId(Long instanceId, String nodeId);
}
