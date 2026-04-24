package com.aiagent.service;

import com.aiagent.entity.WorkflowInstance;
import com.aiagent.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 工作流异步执行器
 * 将异步执行逻辑从 WorkflowEngine 中分离，避免 @Async 方法直接调用同类中的 @Transactional 方法导致 Spring AOP 代理不生效的问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAsyncExecutor {

    private final WorkflowEngine workflowEngine;

    /**
     * 异步执行工作流节点
     * 通过独立的 Bean 调用 WorkflowEngine 的 @Transactional 方法，确保事务代理正确生效
     *
     * @param instanceId 工作流实例ID
     */
    @Async
    public void executeNodeAsync(Long instanceId) {
        try {
            // 直接调用 WorkflowEngine 的 @Transactional 方法
            // 由于是通过 Spring 代理注入的，AOP 代理会正确生效
            workflowEngine.executeNode(instanceId);
        } catch (BusinessException e) {
            log.error("Async node execution failed: instanceId={}, error={}", instanceId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in async node execution: instanceId={}, error={}", instanceId, e.getMessage(), e);
        }
    }
}
