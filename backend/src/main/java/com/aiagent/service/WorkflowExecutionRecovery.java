package com.aiagent.service;

import com.aiagent.config.properties.WorkflowProperties;
import com.aiagent.entity.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流执行恢复器
 *
 * 应用启动时检查所有 RUNNING 状态的工作流实例：
 * - 超过 max-execution-duration 的标记为 FAILED（超时）
 * - 未超时的尝试恢复执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowExecutionRecovery {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowAsyncExecutor workflowAsyncExecutor;
    private final WorkflowProperties workflowProperties;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(rollbackFor = Exception.class)
    public void recoverInterruptedWorkflows() {
        log.info("[WorkflowRecovery] 开始检查中断的工作流实例...");

        List<WorkflowInstance> runningInstances = instanceRepository.findAll().stream()
                .filter(i -> i.getStatus() == WorkflowInstance.InstanceStatus.RUNNING
                        || i.getStatus() == WorkflowInstance.InstanceStatus.PENDING)
                .toList();

        if (runningInstances.isEmpty()) {
            log.info("[WorkflowRecovery] 没有需要恢复的工作流实例");
            return;
        }

        log.info("[WorkflowRecovery] 发现 {} 个中断的工作流实例", runningInstances.size());

        int recovered = 0;
        int failed = 0;

        for (WorkflowInstance instance : runningInstances) {
            if (instance.getStartedAt() == null) {
                // 无启动时间，标记为失败
                instance.setStatus(WorkflowInstance.InstanceStatus.FAILED);
                instance.setError("工作流实例启动时间缺失，标记为失败");
                instance.setCompletedAt(LocalDateTime.now());
                instanceRepository.save(instance);
                failed++;
                continue;
            }

            long elapsedSeconds = Duration.between(instance.getStartedAt(), LocalDateTime.now()).getSeconds();

            if (elapsedSeconds > workflowProperties.getMaxExecutionDuration()) {
                // 超时，标记为失败
                log.warn("[WorkflowRecovery] 实例 {} 已超时（{}s > {}s），标记为 FAILED",
                        instance.getId(), elapsedSeconds, workflowProperties.getMaxExecutionDuration());
                instance.setStatus(WorkflowInstance.InstanceStatus.FAILED);
                instance.setError("工作流执行超时（服务器重启前已运行 " + elapsedSeconds + " 秒）");
                instance.setCompletedAt(LocalDateTime.now());
                instanceRepository.save(instance);
                failed++;
            } else {
                // 未超时，尝试恢复执行
                log.info("[WorkflowRecovery] 恢复实例 {} (当前节点: {}, 已运行: {}s)",
                        instance.getId(), instance.getCurrentNodeId(), elapsedSeconds);
                recovered++;
                // 异步恢复执行（避免阻塞启动过程）
                workflowAsyncExecutor.executeNodeAsync(instance.getId());
            }
        }

        log.info("[WorkflowRecovery] 恢复完成: 恢复执行 {} 个, 标记失败 {} 个", recovered, failed);
    }
}
