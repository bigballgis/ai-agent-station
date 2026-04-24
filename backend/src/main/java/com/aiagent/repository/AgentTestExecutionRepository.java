package com.aiagent.repository;

import com.aiagent.entity.AgentTestExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 根据租户ID分页查询测试执行记录
     */
    @EntityGraph(attributePaths = {"testCase"})
    Page<AgentTestExecution> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * 根据Agent ID查询测试执行记录
     */
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByAgentId(Long agentId);

    /**
     * 根据Agent ID分页查询测试执行记录
     */
    @EntityGraph(attributePaths = {"testCase"})
    Page<AgentTestExecution> findByAgentIdAndTenantId(Long agentId, Long tenantId, Pageable pageable);

    @Deprecated
    @EntityGraph(attributePaths = {"testCase"})
    Page<AgentTestExecution> findByAgentId(Long agentId, Pageable pageable);

    /**
     * 根据测试用例ID查询测试执行记录
     */
    List<AgentTestExecution> findByTestCaseIdAndTenantId(Long testCaseId, Long tenantId);

    @Deprecated
    List<AgentTestExecution> findByTestCaseId(Long testCaseId);

    /**
     * 根据测试用例ID分页查询测试执行记录
     */
    Page<AgentTestExecution> findByTestCaseIdAndTenantId(Long testCaseId, Long tenantId, Pageable pageable);

    @Deprecated
    Page<AgentTestExecution> findByTestCaseId(Long testCaseId, Pageable pageable);

    /**
     * 根据租户ID和状态查询测试执行记录
     * @param tenantId 租户ID
     * @param status 状态
     * @return 测试执行记录列表
     */
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByTenantIdAndStatus(Long tenantId, Integer status);

    /**
     * 根据租户ID和状态分页查询测试执行记录
     */
    @EntityGraph(attributePaths = {"testCase"})
    Page<AgentTestExecution> findByTenantIdAndStatus(Long tenantId, Integer status, Pageable pageable);

    /**
     * 根据租户ID和执行类型查询测试执行记录
     * @param tenantId 租户ID
     * @param executionType 执行类型
     * @return 测试执行记录列表
     */
    @EntityGraph(attributePaths = {"testCase"})
    List<AgentTestExecution> findByTenantIdAndExecutionType(Long tenantId, String executionType);

    /**
     * 根据租户ID和执行类型分页查询测试执行记录
     */
    @EntityGraph(attributePaths = {"testCase"})
    Page<AgentTestExecution> findByTenantIdAndExecutionType(Long tenantId, String executionType, Pageable pageable);

    /**
     * 统计租户的测试执行记录数量
     * @param tenantId 租户ID
     * @return 测试执行记录数量
     */
    long countByTenantId(Long tenantId);

    /**
     * 统计Agent的测试执行记录数量
     */
    long countByAgentIdAndTenantId(Long agentId, Long tenantId);

    @Deprecated
    long countByAgentId(Long agentId);

    /**
     * 统计测试用例的测试执行记录数量
     */
    long countByTestCaseIdAndTenantId(Long testCaseId, Long tenantId);

    @Deprecated
    long countByTestCaseId(Long testCaseId);
}
