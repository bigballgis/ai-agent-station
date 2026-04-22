package com.aiagent.repository;

import com.aiagent.entity.AgentTestExecution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentTestExecutionRepository extends JpaRepository<AgentTestExecution, Long> {

    /**
     * 根据租户ID查询测试执行记录
     * @param tenantId 租户ID
     * @return 测试执行记录列表
     */
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByTenantId(Long tenantId);

    /**
     * 根据Agent ID查询测试执行记录
     * @param agentId Agent ID
     * @return 测试执行记录列表
     */
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByAgentId(Long agentId);

    /**
     * 根据测试用例ID查询测试执行记录
     * @param testCaseId 测试用例ID
     * @return 测试执行记录列表
     */
    List<AgentTestExecution> findByTestCaseId(Long testCaseId);

    /**
     * 根据租户ID和状态查询测试执行记录
     * @param tenantId 租户ID
     * @param status 状态
     * @return 测试执行记录列表
     */
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByTenantIdAndStatus(Long tenantId, Integer status);

    /**
     * 根据租户ID和执行类型查询测试执行记录
     * @param tenantId 租户ID
     * @param executionType 执行类型
     * @return 测试执行记录列表
     */
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByTenantIdAndExecutionType(Long tenantId, String executionType);

    /**
     * 统计租户的测试执行记录数量
     * @param tenantId 租户ID
     * @return 测试执行记录数量
     */
    long countByTenantId(Long tenantId);

    /**
     * 统计Agent的测试执行记录数量
     * @param agentId Agent ID
     * @return 测试执行记录数量
     */
    long countByAgentId(Long agentId);

    /**
     * 统计测试用例的测试执行记录数量
     * @param testCaseId 测试用例ID
     * @return 测试执行记录数量
     */
    long countByTestCaseId(Long testCaseId);
}
