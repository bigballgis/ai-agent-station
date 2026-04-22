package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentEvolutionReflection;
import com.aiagent.entity.AgentEvolutionSuggestion;
import com.aiagent.repository.AgentEvolutionReflectionRepository;
import com.aiagent.repository.AgentEvolutionSuggestionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReflectionEvaluationService 单元测试
 * 测试Agent执行反思评估、性能评分计算、准确性评分计算等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("反思评估服务测试")
class ReflectionEvaluationServiceTest {

    @Mock
    private AgentEvolutionReflectionRepository reflectionRepository;

    @Mock
    private AgentEvolutionSuggestionRepository suggestionRepository;

    @InjectMocks
    private ReflectionEvaluationService reflectionEvaluationService;

    private Agent testAgent;
    private Map<String, Object> executionContext;

    @BeforeEach
    void setUp() {
        // 初始化测试Agent数据
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setTenantId(100L);
        testAgent.setName("测试Agent");
        testAgent.setDescription("测试描述");

        // 初始化执行上下文
        executionContext = new HashMap<>();
        executionContext.put("inputs", Collections.singletonMap("query", "测试查询"));
        executionContext.put("executionTime", 500L);
    }

    // ==================== evaluate 测试 ====================

    @Test
    @DisplayName("评估执行 - 成功执行时评分正确")
    void evaluateExecution_SuccessExecution() {
        when(reflectionRepository.save(any(AgentEvolutionReflection.class)))
                .thenAnswer(invocation -> {
                    AgentEvolutionReflection r = invocation.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "success result", null, 500L, 100L, 1L);

        assertNotNull(result);
        // 成功执行，性能评分应为5.0（<1000ms）
        assertEquals(0, new BigDecimal("5.0").compareTo(result.getPerformanceScore()));
        // 准确性评分应为4.5（包含success）
        assertEquals(0, new BigDecimal("4.5").compareTo(result.getAccuracyScore()));
        // 效率评分应为5.0（<1000ms）
        assertEquals(0, new BigDecimal("5.0").compareTo(result.getEfficiencyScore()));
        // 用户满意度应为4.5
        assertEquals(0, new BigDecimal("4.5").compareTo(result.getUserSatisfactionScore()));
        // 状态应为已完成
        assertEquals(1, result.getStatus());
        verify(reflectionRepository).save(any(AgentEvolutionReflection.class));
    }

    @Test
    @DisplayName("评估执行 - 执行失败时评分较低")
    void evaluateExecution_FailedExecution() {
        when(reflectionRepository.save(any(AgentEvolutionReflection.class)))
                .thenAnswer(invocation -> {
                    AgentEvolutionReflection r = invocation.getArgument(0);
                    r.setId(2L);
                    return r;
                });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, null, "执行超时", 5000L, 100L, 1L);

        assertNotNull(result);
        // 执行失败，性能评分应为2.0
        assertEquals(0, new BigDecimal("2.0").compareTo(result.getPerformanceScore()));
        // 执行失败，准确性评分应为1.0
        assertEquals(0, new BigDecimal("1.0").compareTo(result.getAccuracyScore()));
        // 执行失败，用户满意度应为1.0
        assertEquals(0, new BigDecimal("1.0").compareTo(result.getUserSatisfactionScore()));
        // 应包含错误劣势
        assertTrue(result.getWeaknesses().stream().anyMatch(w -> w.contains("执行超时")));
    }

    @Test
    @DisplayName("评估执行 - 执行结果为空时包含劣势")
    void evaluateExecution_EmptyResult() {
        when(reflectionRepository.save(any(AgentEvolutionReflection.class)))
                .thenAnswer(invocation -> {
                    AgentEvolutionReflection r = invocation.getArgument(0);
                    r.setId(3L);
                    return r;
                });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "", null, 2000L, 100L, 1L);

        assertNotNull(result);
        // 空结果应包含劣势
        assertTrue(result.getWeaknesses().stream().anyMatch(w -> w.contains("执行结果为空")));
    }

    @Test
    @DisplayName("评估执行 - 包含输入时应分析优势")
    void evaluateExecution_WithInputs() {
        when(reflectionRepository.save(any(AgentEvolutionReflection.class)))
                .thenAnswer(invocation -> {
                    AgentEvolutionReflection r = invocation.getArgument(0);
                    r.setId(4L);
                    return r;
                });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "success", null, 500L, 100L, 1L);

        assertNotNull(result);
        // 包含inputs时应包含"能够正确处理输入参数"优势
        assertTrue(result.getStrengths().stream()
                .anyMatch(s -> s.contains("能够正确处理输入参数")));
    }

    // ==================== calculatePerformanceScore 测试 ====================

    @Test
    @DisplayName("计算性能评分 - 执行时间小于1秒返回5.0")
    void calculatePerformanceScore_Fast() {
        when(reflectionRepository.save(any())).thenAnswer(inv -> {
            AgentEvolutionReflection r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "success", null, 800L, 100L, 1L);

        assertEquals(0, new BigDecimal("5.0").compareTo(result.getPerformanceScore()));
    }

    @Test
    @DisplayName("计算性能评分 - 执行时间1-3秒返回4.0")
    void calculatePerformanceScore_Normal() {
        when(reflectionRepository.save(any())).thenAnswer(inv -> {
            AgentEvolutionReflection r = inv.getArgument(0);
            r.setId(11L);
            return r;
        });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "success", null, 2000L, 100L, 1L);

        assertEquals(0, new BigDecimal("4.0").compareTo(result.getPerformanceScore()));
    }

    @Test
    @DisplayName("计算性能评分 - 执行时间3-5秒返回3.0")
    void calculatePerformanceScore_Slow() {
        when(reflectionRepository.save(any())).thenAnswer(inv -> {
            AgentEvolutionReflection r = inv.getArgument(0);
            r.setId(12L);
            return r;
        });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "success", null, 4000L, 100L, 1L);

        assertEquals(0, new BigDecimal("3.0").compareTo(result.getPerformanceScore()));
    }

    @Test
    @DisplayName("计算性能评分 - 执行时间超过5秒返回2.0")
    void calculatePerformanceScore_VerySlow() {
        when(reflectionRepository.save(any())).thenAnswer(inv -> {
            AgentEvolutionReflection r = inv.getArgument(0);
            r.setId(13L);
            return r;
        });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "success", null, 6000L, 100L, 1L);

        assertEquals(0, new BigDecimal("2.0").compareTo(result.getPerformanceScore()));
    }

    // ==================== calculateAccuracyScore 测试 ====================

    @Test
    @DisplayName("计算准确性评分 - 包含success时返回4.5")
    void calculateAccuracyScore_SuccessResult() {
        when(reflectionRepository.save(any())).thenAnswer(inv -> {
            AgentEvolutionReflection r = inv.getArgument(0);
            r.setId(20L);
            return r;
        });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "operation success", null, 500L, 100L, 1L);

        assertEquals(0, new BigDecimal("4.5").compareTo(result.getAccuracyScore()));
    }

    @Test
    @DisplayName("计算准确性评分 - 结果不包含success时返回3.0")
    void calculateAccuracyScore_UnclearResult() {
        when(reflectionRepository.save(any())).thenAnswer(inv -> {
            AgentEvolutionReflection r = inv.getArgument(0);
            r.setId(21L);
            return r;
        });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, "some result", null, 500L, 100L, 1L);

        assertEquals(0, new BigDecimal("3.0").compareTo(result.getAccuracyScore()));
    }

    @Test
    @DisplayName("计算准确性评分 - 有错误信息时返回1.0")
    void calculateAccuracyScore_WithError() {
        when(reflectionRepository.save(any())).thenAnswer(inv -> {
            AgentEvolutionReflection r = inv.getArgument(0);
            r.setId(22L);
            return r;
        });

        AgentEvolutionReflection result = reflectionEvaluationService.evaluateExecution(
                testAgent, executionContext, null, "error occurred", 500L, 100L, 1L);

        assertEquals(0, new BigDecimal("1.0").compareTo(result.getAccuracyScore()));
    }

    // ==================== 其他功能测试 ====================

    @Test
    @DisplayName("获取Agent反思历史 - 成功")
    void getAgentReflections_Success() {
        AgentEvolutionReflection reflection = new AgentEvolutionReflection();
        reflection.setId(1L);
        reflection.setAgentId(1L);
        reflection.setTenantId(100L);

        when(reflectionRepository.findByAgentIdAndTenantId(1L, 100L))
                .thenReturn(Arrays.asList(reflection));

        List<AgentEvolutionReflection> result = reflectionEvaluationService.getAgentReflections(1L, 100L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取反思改进建议 - 成功")
    void getReflectionSuggestions_Success() {
        AgentEvolutionSuggestion suggestion = new AgentEvolutionSuggestion();
        suggestion.setId(1L);
        suggestion.setReflectionId(1L);

        when(suggestionRepository.findByReflectionId(1L))
                .thenReturn(Arrays.asList(suggestion));

        List<AgentEvolutionSuggestion> result = reflectionEvaluationService.getReflectionSuggestions(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("更新建议实现状态 - 成功")
    void updateSuggestionStatus_Success() {
        AgentEvolutionSuggestion suggestion = new AgentEvolutionSuggestion();
        suggestion.setId(1L);
        suggestion.setStatus("PENDING");
        suggestion.setImplementationStatus("NOT_IMPLEMENTED");

        when(suggestionRepository.findById(1L)).thenReturn(Optional.of(suggestion));
        when(suggestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reflectionEvaluationService.updateSuggestionStatus(1L, "IMPLEMENTED", 1L);

        verify(suggestionRepository).save(argThat(s ->
                "IMPLEMENTED".equals(s.getImplementationStatus()) &&
                        s.getImplementedBy().equals(1L) &&
                        s.getImplementedAt() != null
        ));
    }

    @Test
    @DisplayName("更新建议实现状态 - 建议不存在时抛出异常")
    void updateSuggestionStatus_NotFound_ThrowsException() {
        when(suggestionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> reflectionEvaluationService.updateSuggestionStatus(999L, "IMPLEMENTED", 1L));
    }
}
