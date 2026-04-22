package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.entity.Agent.AgentStatus;
import com.aiagent.entity.AgentVersion;
import com.aiagent.entity.DeploymentHistory;
import com.aiagent.entity.DeploymentHistory.DeploymentStatus;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentVersionRepository;
import com.aiagent.repository.DeploymentHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DeploymentService 单元测试
 * 测试部署、回滚、部署历史查询等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("部署服务测试")
class DeploymentServiceTest {

    @Mock
    private DeploymentHistoryRepository deploymentHistoryRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentVersionRepository agentVersionRepository;

    @InjectMocks
    private DeploymentService deploymentService;

    private Agent testAgent;
    private AgentVersion testVersion;

    @BeforeEach
    void setUp() {
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setTenantId(100L);
        testAgent.setName("测试Agent");
        testAgent.setStatus(AgentStatus.APPROVED);
        testAgent.setConfig(Map.of("model", "gpt-4", "temperature", "0.7"));

        testVersion = new AgentVersion();
        testVersion.setId(1L);
        testVersion.setAgentId(1L);
        testVersion.setVersionNumber(1);
        testVersion.setConfig(Map.of("model", "gpt-4"));
    }

    private DeploymentHistory createDeployment(Long id, DeploymentStatus status, String version) {
        DeploymentHistory deployment = new DeploymentHistory();
        deployment.setId(id);
        deployment.setAgentId(1L);
        deployment.setTenantId(100L);
        deployment.setAgentVersionId(1L);
        deployment.setDeployerId(1L);
        deployment.setStatus(status);
        deployment.setVersion(version);
        return deployment;
    }

    @Test
    @DisplayName("获取部署历史 - 成功")
    void testGetDeploymentHistory_Success() {
        DeploymentHistory d1 = createDeployment(1L, DeploymentStatus.SUCCESS, "1.0.0");
        DeploymentHistory d2 = createDeployment(2L, DeploymentStatus.FAILED, "1.0.1");
        Pageable pageable = PageRequest.of(0, 10);
        Page<DeploymentHistory> page = new PageImpl<>(List.of(d1, d2));

        when(deploymentHistoryRepository.findByTenantId(eq(100L), any(Pageable.class))).thenReturn(page);

        Page<DeploymentHistory> result = deploymentService.getDeploymentHistory(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    @DisplayName("根据Agent ID获取部署历史 - 成功")
    void testGetDeploymentHistoryByAgentId_Success() {
        DeploymentHistory d1 = createDeployment(1L, DeploymentStatus.SUCCESS, "1.0.0");
        when(deploymentHistoryRepository.findByAgentIdAndTenantIdOrderByCreatedAtDesc(eq(1L), eq(100L)))
                .thenReturn(List.of(d1));

        List<DeploymentHistory> result = deploymentService.getDeploymentHistoryByAgentId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("根据ID获取部署详情 - 成功")
    void testGetDeploymentById_Success() {
        DeploymentHistory d1 = createDeployment(1L, DeploymentStatus.SUCCESS, "1.0.0");
        when(deploymentHistoryRepository.findByIdAndTenantId(eq(1L), eq(100L)))
                .thenReturn(Optional.of(d1));

        DeploymentHistory result = deploymentService.getDeploymentById(1L);

        assertNotNull(result);
        assertEquals(DeploymentStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("根据ID获取部署详情 - 不存在抛出异常")
    void testGetDeploymentById_NotFound() {
        when(deploymentHistoryRepository.findByIdAndTenantId(eq(999L), eq(100L)))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            deploymentService.getDeploymentById(999L);
        });
    }

    @Test
    @DisplayName("部署Agent - 成功")
    void testDeploy_Success() {
        when(agentRepository.findByIdAndTenantId(eq(1L), eq(100L))).thenReturn(Optional.of(testAgent));
        when(agentVersionRepository.findById(eq(1L))).thenReturn(Optional.of(testVersion));
        when(agentVersionRepository.findByAgentIdAndTenantIdOrderByVersionNumberDesc(eq(1L), eq(100L)))
                .thenReturn(List.of());
        when(deploymentHistoryRepository.save(any(DeploymentHistory.class)))
                .thenAnswer(invocation -> {
                    DeploymentHistory d = invocation.getArgument(0);
                    d.setId(1L);
                    return d;
                });
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeploymentHistory result = deploymentService.deploy(1L, 1L, false, 100, "首次部署", 1L);

        assertNotNull(result);
        assertEquals(DeploymentStatus.SUCCESS, result.getStatus());
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    @DisplayName("部署Agent - Agent不存在抛出异常")
    void testDeploy_AgentNotFound() {
        when(agentRepository.findByIdAndTenantId(eq(1L), eq(100L))).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            deploymentService.deploy(1L, 1L, false, 100, "部署", 1L);
        });
    }

    @Test
    @DisplayName("部署Agent - Agent状态不允许发布")
    void testDeploy_InvalidStatus() {
        testAgent.setStatus(AgentStatus.DRAFT);
        when(agentRepository.findByIdAndTenantId(eq(1L), eq(100L))).thenReturn(Optional.of(testAgent));

        assertThrows(BusinessException.class, () -> {
            deploymentService.deploy(1L, 1L, false, 100, "部署", 1L);
        });
    }

    @Test
    @DisplayName("部署Agent - 版本不存在抛出异常")
    void testDeploy_VersionNotFound() {
        when(agentRepository.findByIdAndTenantId(eq(1L), eq(100L))).thenReturn(Optional.of(testAgent));
        when(agentVersionRepository.findById(eq(1L))).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            deploymentService.deploy(1L, 1L, false, 100, "部署", 1L);
        });
    }

    @Test
    @DisplayName("回滚部署 - 成功")
    void testRollback_Success() {
        DeploymentHistory currentDeployment = createDeployment(2L, DeploymentStatus.SUCCESS, "1.0.1");
        DeploymentHistory previousDeployment = createDeployment(1L, DeploymentStatus.SUCCESS, "1.0.0");
        previousDeployment.setAgentVersionId(1L);

        when(deploymentHistoryRepository.findByIdAndTenantId(eq(2L), eq(100L)))
                .thenReturn(Optional.of(currentDeployment));
        when(deploymentHistoryRepository.findSuccessfulDeployments(eq(100L), eq(1L)))
                .thenReturn(List.of(currentDeployment, previousDeployment));
        when(agentVersionRepository.findById(eq(1L))).thenReturn(Optional.of(testVersion));
        when(agentRepository.findByIdAndTenantId(eq(1L), eq(100L))).thenReturn(Optional.of(testAgent));
        when(deploymentHistoryRepository.save(any(DeploymentHistory.class)))
                .thenAnswer(invocation -> {
                    DeploymentHistory d = invocation.getArgument(0);
                    if (d.getId() == null) d.setId(3L);
                    return d;
                });
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeploymentHistory result = deploymentService.rollback(2L, 1L);

        assertNotNull(result);
        assertEquals(DeploymentStatus.SUCCESS, result.getStatus());
        assertEquals(DeploymentStatus.ROLLED_BACK, currentDeployment.getStatus());
    }

    @Test
    @DisplayName("回滚部署 - 只有一次成功部署时抛出异常")
    void testRollback_NoPreviousVersion() {
        DeploymentHistory currentDeployment = createDeployment(1L, DeploymentStatus.SUCCESS, "1.0.0");

        when(deploymentHistoryRepository.findByIdAndTenantId(eq(1L), eq(100L)))
                .thenReturn(Optional.of(currentDeployment));
        when(deploymentHistoryRepository.findSuccessfulDeployments(eq(100L), eq(1L)))
                .thenReturn(List.of(currentDeployment));

        assertThrows(BusinessException.class, () -> {
            deploymentService.rollback(1L, 1L);
        });
    }

    @Test
    @DisplayName("回滚部署 - 只能回滚成功的部署")
    void testRollback_NotSuccessful() {
        DeploymentHistory failedDeployment = createDeployment(1L, DeploymentStatus.FAILED, "1.0.0");

        when(deploymentHistoryRepository.findByIdAndTenantId(eq(1L), eq(100L)))
                .thenReturn(Optional.of(failedDeployment));

        assertThrows(BusinessException.class, () -> {
            deploymentService.rollback(1L, 1L);
        });
    }

    @Test
    @DisplayName("比较版本 - 成功")
    void testCompareVersions_Success() {
        AgentVersion v1 = new AgentVersion();
        v1.setId(1L);
        v1.setConfig(Map.of("key1", "value1", "key2", "value2"));

        AgentVersion v2 = new AgentVersion();
        v2.setId(2L);
        v2.setConfig(Map.of("key1", "value1", "key3", "value3"));

        when(agentVersionRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(agentVersionRepository.findById(2L)).thenReturn(Optional.of(v2));

        Map<String, Object> result = deploymentService.compareVersions(1L, 2L);

        assertNotNull(result);
        assertTrue(result.containsKey("version1"));
        assertTrue(result.containsKey("version2"));
        assertTrue(result.containsKey("configDiff"));
    }
}
