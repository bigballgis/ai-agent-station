package com.aiagent.repository;

import com.aiagent.entity.AgentTestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentTestCaseRepository extends JpaRepository<AgentTestCase, Long> {

    /**
     * 根据租户ID查询测试用例
     * @param tenantId 租户ID
     * @return 测试用例列表
     */
    List<AgentTestCase> findByTenantId(Long tenantId);

    /**
     * 根据租户ID分页查询测试用例
     */
    Page<AgentTestCase> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * 根据Agent ID查询测试用例
     * @param agentId Agent ID
     * @return 测试用例列表
     */
    List<AgentTestCase> findByAgentId(Long agentId);

    /**
     * 根据Agent ID分页查询测试用例
     */
    Page<AgentTestCase> findByAgentId(Long agentId, Pageable pageable);

    /**
     * 根据租户ID和测试代码查询测试用例
     * @param tenantId 租户ID
     * @param testCode 测试代码
     * @return 测试用例
     */
    AgentTestCase findByTenantIdAndTestCode(Long tenantId, String testCode);

    /**
     * 根据租户ID和状态查询测试用例
     * @param tenantId 租户ID
     * @param status 状态
     * @return 测试用例列表
     */
    List<AgentTestCase> findByTenantIdAndStatus(Long tenantId, Integer status);

    /**
     * 根据租户ID和状态分页查询测试用例
     */
    Page<AgentTestCase> findByTenantIdAndStatus(Long tenantId, Integer status, Pageable pageable);

    /**
     * 根据租户ID和测试类型查询测试用例
     * @param tenantId 租户ID
     * @param testType 测试类型
     * @return 测试用例列表
     */
    List<AgentTestCase> findByTenantIdAndTestType(Long tenantId, String testType);

    /**
     * 根据租户ID和测试类型分页查询测试用例
     */
    Page<AgentTestCase> findByTenantIdAndTestType(Long tenantId, String testType, Pageable pageable);

    /**
     * 统计租户的测试用例数量
     * @param tenantId 租户ID
     * @return 测试用例数量
     */
    long countByTenantId(Long tenantId);

    /**
     * 统计Agent的测试用例数量
     * @param agentId Agent ID
     * @return 测试用例数量
     */
    long countByAgentId(Long agentId);
}
