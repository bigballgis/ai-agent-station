package com.aiagent;

import com.aiagent.dto.AgentInvokeRequest;
import com.aiagent.dto.AgentInvokeResponse;
import com.aiagent.engine.AgentExecutionEngine;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentApproval;
import com.aiagent.entity.AgentVersion;
import com.aiagent.entity.DeploymentHistory;
import com.aiagent.repository.AgentRepository;
import com.aiagent.service.AgentApprovalService;
import com.aiagent.service.DeploymentService;
import com.aiagent.tenant.TenantContextHolder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Integration test requires full Spring environment (DB/Redis). Enable in dedicated integration test pipeline.")
public class IntegrationTest {

    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private AgentExecutionEngine agentExecutionEngine;
    
    @Autowired
    private AgentApprovalService agentApprovalService;
    
    @Autowired
    private DeploymentService deploymentService;
    
    private Agent testAgent;
    private AgentVersion testVersion;
    
    @BeforeEach
    public void setup() {
        // 设置租户上下文
        TenantContextHolder.setTenantId(1L);
        
        // 创建测试Agent
        testAgent = new Agent();
        testAgent.setTenantId(1L);
        testAgent.setName("Test Agent");
        testAgent.setDescription("Test Agent for integration testing");
        testAgent.setStatus(Agent.AgentStatus.DRAFT);
        testAgent.setCreatedBy(1L);
        testAgent = agentRepository.save(testAgent);
        
        // 创建测试版本
        testVersion = new AgentVersion();
        testVersion.setAgentId(testAgent.getId());
        testVersion.setTenantId(1L);
        testVersion.setVersionNumber(1);
        testVersion.setConfig(new HashMap<>());
        testVersion.setCreatedBy(1L);
    }
    
    @Test
    public void testLangGraphIntegration() {
        // 测试LangGraph执行引擎集成
        AgentInvokeRequest request = new AgentInvokeRequest();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("message", "Hello, world!");
        request.setInputs(inputs);
        request.setAsync(false);
        
        // 先将Agent状态设置为已发布
        testAgent.setStatus(Agent.AgentStatus.PUBLISHED);
        agentRepository.save(testAgent);
        
        // 执行Agent
        AgentInvokeResponse response = agentExecutionEngine.invokeAgent(testAgent.getId(), request, "test-request-1", 1L);
        
        // 验证执行结果
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getOutputs());
        assertTrue(response.getOutputs().containsKey("message"));
    }
    
    @Test
    public void testApprovalProcess() {
        // 测试审批流程
        AgentApproval approval = agentApprovalService.submitForApproval(testAgent.getId(), testVersion.getId(), "Test approval", 1L);
        
        // 验证审批状态
        assertEquals(AgentApproval.ApprovalStatus.PENDING, approval.getStatus());
        
        // 审批通过
        approval = agentApprovalService.approve(approval.getId(), "Approved", 2L);
        
        // 验证审批状态
        assertEquals(AgentApproval.ApprovalStatus.APPROVED, approval.getStatus());
        
        // 验证Agent状态
        Agent updatedAgent = agentRepository.findById(testAgent.getId()).orElse(null);
        assertNotNull(updatedAgent);
        assertEquals(Agent.AgentStatus.APPROVED, updatedAgent.getStatus());
    }
    
    @Test
    public void testDeploymentProcess() {
        // 先将Agent状态设置为已审批
        testAgent.setStatus(Agent.AgentStatus.APPROVED);
        agentRepository.save(testAgent);
        
        // 测试发布流程
        DeploymentHistory deployment = deploymentService.deploy(testAgent.getId(), testVersion.getId(), false, 100, "Test deployment", 1L);
        
        // 验证发布状态
        assertEquals(DeploymentHistory.DeploymentStatus.SUCCESS, deployment.getStatus());
        
        // 验证Agent状态
        Agent updatedAgent = agentRepository.findById(testAgent.getId()).orElse(null);
        assertNotNull(updatedAgent);
        assertEquals(Agent.AgentStatus.PUBLISHED, updatedAgent.getStatus());
    }
    
    @Test
    public void testCompleteFlow() {
        // 测试完整流程：提交审批 → 审批通过 → 发布
        
        // 1. 提交审批
        AgentApproval approval = agentApprovalService.submitForApproval(testAgent.getId(), testVersion.getId(), "Test approval", 1L);
        assertEquals(AgentApproval.ApprovalStatus.PENDING, approval.getStatus());
        
        // 2. 审批通过
        approval = agentApprovalService.approve(approval.getId(), "Approved", 2L);
        assertEquals(AgentApproval.ApprovalStatus.APPROVED, approval.getStatus());
        
        // 3. 发布
        DeploymentHistory deployment = deploymentService.deploy(testAgent.getId(), testVersion.getId(), false, 100, "Test deployment", 1L);
        assertEquals(DeploymentHistory.DeploymentStatus.SUCCESS, deployment.getStatus());
        
        // 4. 验证Agent状态
        Agent updatedAgent = agentRepository.findById(testAgent.getId()).orElse(null);
        assertNotNull(updatedAgent);
        assertEquals(Agent.AgentStatus.PUBLISHED, updatedAgent.getStatus());
        
        // 5. 执行Agent
        AgentInvokeRequest request = new AgentInvokeRequest();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("message", "Hello from complete flow!");
        request.setInputs(inputs);
        request.setAsync(false);
        
        AgentInvokeResponse response = agentExecutionEngine.invokeAgent(testAgent.getId(), request, "test-request-2", 1L);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getOutputs());
    }
}
