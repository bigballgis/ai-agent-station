package com.aiagent.service;

import com.aiagent.entity.AgentTestResult;
import com.aiagent.exception.ResourceNotFoundException;
import com.aiagent.repository.AgentTestResultRepository;
import com.aiagent.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AgentTestResultService {

    private static final Logger log = LoggerFactory.getLogger(AgentTestResultService.class);
    private final AgentTestResultRepository resultRepository;

    public AgentTestResultService(AgentTestResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Optional<AgentTestResult> getResultById(Long id) {
        return resultRepository.findById(id);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestResult> getResultsByExecutionId(Long executionId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return resultRepository.findByExecutionIdAndTenantId(executionId, tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestResult> getResultsByTenantId(Long tenantId) {
        return resultRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestResult> getResultsByAgentId(Long agentId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return resultRepository.findByAgentIdAndTenantId(agentId, tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestResult> getResultsByTestCaseId(Long testCaseId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return resultRepository.findByTestCaseIdAndTenantId(testCaseId, tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestResult> getResultsByStatus(Long tenantId, String status) {
        return resultRepository.findByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTestResult updateResult(Long id, AgentTestResult updatedResult) {
        AgentTestResult existingResult = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("测试结果不存在"));

        existingResult.setActualOutput(updatedResult.getActualOutput());
        existingResult.setExpectedOutput(updatedResult.getExpectedOutput());
        existingResult.setStatus(updatedResult.getStatus());
        existingResult.setComparisonResult(updatedResult.getComparisonResult());
        existingResult.setErrorMessage(updatedResult.getErrorMessage());

        log.info("Updating test result: {}", existingResult.getId());
        return resultRepository.save(existingResult);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long countResultsByTenant(Long tenantId) {
        return resultRepository.countByTenantId(tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long countResultsByAgent(Long agentId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return resultRepository.countByAgentIdAndTenantId(agentId, tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long countResultsByTestCase(Long testCaseId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return resultRepository.countByTestCaseIdAndTenantId(testCaseId, tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long countResultsByExecution(Long executionId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return resultRepository.countByExecutionIdAndTenantId(executionId, tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public double getPassRateByAgent(Long agentId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        long total = resultRepository.countByAgentIdAndTenantId(agentId, tenantId);
        if (total == 0) {
            return 0.0;
        }
        long passed = resultRepository.countByAgentIdAndStatus(agentId, "SUCCESS");
        return (double) passed / total * 100;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public double getPassRateByTestCase(Long testCaseId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        long total = resultRepository.countByTestCaseIdAndTenantId(testCaseId, tenantId);
        if (total == 0) {
            return 0.0;
        }
        long passed = resultRepository.countByTestCaseIdAndStatus(testCaseId, "SUCCESS");
        return (double) passed / total * 100;
    }
}
