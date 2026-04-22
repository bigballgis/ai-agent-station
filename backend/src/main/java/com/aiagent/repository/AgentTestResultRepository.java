package com.aiagent.repository;

import com.aiagent.entity.AgentTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentTestResultRepository extends JpaRepository<AgentTestResult, Long> {

    /**
     * 根据执行ID查询测试结果
     * @param executionId 执行ID
     * @return 测试结果列表
     */
    List<AgentTestResult> findByExecutionId(Long executionId);

    /**
     * 根据租户ID查询测试结果
     * @param tenantId 租户ID
     * @return 测试结果列表
     */
    List<AgentTestResult> findByTenantId(Long tenantId);

    /**
     * 根据Agent ID查询测试结果
     * @param agentId Agent ID
     * @return 测试结果列表
     */
    List<AgentTestResult> findByAgentId(Long agentId);

    /**
     * 根据测试用例ID查询测试结果
     * @param testCaseId 测试用例ID
     * @return 测试结果列表
     */
    List<AgentTestResult> findByTestCaseId(Long testCaseId);

    /**
     * 根据租户ID和状态查询测试结果
     * @param tenantId 租户ID
     * @param status 状态
     * @return 测试结果列表
     */
    List<AgentTestResult> findByTenantIdAndStatus(Long tenantId, String status);

    /**
     * 统计租户的测试结果数量
     * @param tenantId 租户ID
     * @return 测试结果数量
     */
    long countByTenantId(Long tenantId);

    /**
     * 统计Agent的测试结果数量
     * @param agentId Agent ID
     * @return 测试结果数量
     */
    long countByAgentId(Long agentId);

    /**
     * 统计测试用例的测试结果数量
     * @param testCaseId 测试用例ID
     * @return 测试结果数量
     */
    long countByTestCaseId(Long testCaseId);

    /**
     * 统计执行的测试结果数量
     * @param executionId 执行ID
     * @return 测试结果数量
     */
    long countByExecutionId(Long executionId);
}