package com.aiagent.service;

import com.aiagent.entity.AgentTestCase;
import com.aiagent.repository.AgentTestCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AgentTestCaseService {

    private static final Logger log = LoggerFactory.getLogger(AgentTestCaseService.class);
    private final AgentTestCaseRepository testCaseRepository;

    public AgentTestCaseService(AgentTestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTestCase createTestCase(AgentTestCase testCase) {
        log.info("Creating test case: {}", testCase.getTestName());
        return testCaseRepository.save(testCase);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Optional<AgentTestCase> getTestCaseById(Long id) {
        return testCaseRepository.findById(id);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestCase> getTestCasesByTenantId(Long tenantId) {
        return testCaseRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Page<AgentTestCase> getTestCasesByTenantId(Long tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return testCaseRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestCase> getTestCasesByAgentId(Long agentId) {
        return testCaseRepository.findByAgentId(agentId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Page<AgentTestCase> getTestCasesByAgentId(Long agentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return testCaseRepository.findByAgentId(agentId, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTestCase updateTestCase(Long id, AgentTestCase updatedTestCase) {
        AgentTestCase existingTestCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));

        existingTestCase.setTestName(updatedTestCase.getTestName());
        existingTestCase.setDescription(updatedTestCase.getDescription());
        existingTestCase.setTestType(updatedTestCase.getTestType());
        existingTestCase.setInputParams(updatedTestCase.getInputParams());
        existingTestCase.setExpectedOutput(updatedTestCase.getExpectedOutput());
        existingTestCase.setStatus(updatedTestCase.getStatus());

        log.info("Updating test case: {}", existingTestCase.getTestName());
        return testCaseRepository.save(existingTestCase);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTestCase(Long id) {
        AgentTestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));
        log.info("Deleting test case: {}", testCase.getTestName());
        testCaseRepository.delete(testCase);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AgentTestCase getTestCaseByCode(Long tenantId, String testCode) {
        return testCaseRepository.findByTenantIdAndTestCode(tenantId, testCode);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestCase> getTestCasesByStatus(Long tenantId, Integer status) {
        return testCaseRepository.findByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Page<AgentTestCase> getTestCasesByStatus(Long tenantId, Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return testCaseRepository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<AgentTestCase> getTestCasesByType(Long tenantId, String testType) {
        return testCaseRepository.findByTenantIdAndTestType(tenantId, testType);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Page<AgentTestCase> getTestCasesByType(Long tenantId, String testType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return testCaseRepository.findByTenantIdAndTestType(tenantId, testType, pageable);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long countTestCasesByTenant(Long tenantId) {
        return testCaseRepository.countByTenantId(tenantId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long countTestCasesByAgent(Long agentId) {
        return testCaseRepository.countByAgentId(agentId);
    }
}
