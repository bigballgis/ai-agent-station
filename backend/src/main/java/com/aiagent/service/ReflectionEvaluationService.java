package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentEvolutionReflection;
import com.aiagent.entity.AgentEvolutionSuggestion;
import com.aiagent.repository.AgentEvolutionReflectionRepository;
import com.aiagent.repository.AgentEvolutionSuggestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReflectionEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(ReflectionEvaluationService.class);

    private final AgentEvolutionReflectionRepository reflectionRepository;
    private final AgentEvolutionSuggestionRepository suggestionRepository;

    public ReflectionEvaluationService(AgentEvolutionReflectionRepository reflectionRepository, AgentEvolutionSuggestionRepository suggestionRepository) {
        this.reflectionRepository = reflectionRepository;
        this.suggestionRepository = suggestionRepository;
    }

    /**
     * 评估Agent执行结果并生成反思报告
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentEvolutionReflection evaluateExecution(Agent agent, Map<String, Object> executionContext, 
                                                   String executionResult, String errorMessage, 
                                                   long executionTime, Long tenantId, Long userId) {
        log.info("Evaluating agent execution for agent: {}", agent.getName());

        // 创建反思评估记录
        AgentEvolutionReflection reflection = new AgentEvolutionReflection();
        reflection.setTenantId(tenantId);
        reflection.setAgentId(agent.getId());
        reflection.setEvaluationType("EXECUTION_REFLECTION");
        reflection.setCreatedBy(userId);
        reflection.setStatus(1); // 1: 已完成

        // 计算各项评分
        BigDecimal performanceScore = calculatePerformanceScore(executionTime, errorMessage);
        BigDecimal accuracyScore = calculateAccuracyScore(executionResult, errorMessage);
        BigDecimal efficiencyScore = calculateEfficiencyScore(executionTime);
        BigDecimal userSatisfactionScore = calculateUserSatisfactionScore(executionResult, errorMessage);

        reflection.setPerformanceScore(performanceScore);
        reflection.setAccuracyScore(accuracyScore);
        reflection.setEfficiencyScore(efficiencyScore);
        reflection.setUserSatisfactionScore(userSatisfactionScore);

        // 分析优势和劣势
        List<String> strengths = analyzeStrengths(executionContext, executionResult, errorMessage);
        List<String> weaknesses = analyzeWeaknesses(executionContext, executionResult, errorMessage);

        reflection.setStrengths(strengths);
        reflection.setWeaknesses(weaknesses);

        // 生成评估摘要
        String summary = generateSummary(agent, executionResult, errorMessage, performanceScore, 
                                      accuracyScore, efficiencyScore, userSatisfactionScore, 
                                      strengths, weaknesses);
        reflection.setSummary(summary);

        // 保存反思评估记录
        reflection = reflectionRepository.save(reflection);

        // 生成改进建议
        generateImprovementSuggestions(agent, reflection, executionContext, executionResult, 
                                     errorMessage, tenantId, userId);

        return reflection;
    }

    /**
     * 计算性能评分
     */
    private BigDecimal calculatePerformanceScore(long executionTime, String errorMessage) {
        if (errorMessage != null) {
            return new BigDecimal("2.0"); // 执行失败，评分较低
        }

        // 基于执行时间计算性能评分
        if (executionTime < 1000) {
            return new BigDecimal("5.0"); // 优秀
        } else if (executionTime < 3000) {
            return new BigDecimal("4.0"); // 良好
        } else if (executionTime < 5000) {
            return new BigDecimal("3.0"); // 一般
        } else {
            return new BigDecimal("2.0"); // 较差
        }
    }

    /**
     * 计算准确性评分
     */
    private BigDecimal calculateAccuracyScore(String executionResult, String errorMessage) {
        if (errorMessage != null) {
            return new BigDecimal("1.0"); // 执行失败，准确性差
        }

        if (executionResult != null && executionResult.contains("success")) {
            return new BigDecimal("4.5"); // 执行成功
        } else {
            return new BigDecimal("3.0"); // 执行结果不明确
        }
    }

    /**
     * 计算效率评分
     */
    private BigDecimal calculateEfficiencyScore(long executionTime) {
        if (executionTime < 1000) {
            return new BigDecimal("5.0"); // 高效
        } else if (executionTime < 3000) {
            return new BigDecimal("4.0"); // 良好
        } else if (executionTime < 5000) {
            return new BigDecimal("3.0"); // 一般
        } else {
            return new BigDecimal("2.0"); // 低效
        }
    }

    /**
     * 计算用户满意度评分
     */
    private BigDecimal calculateUserSatisfactionScore(String executionResult, String errorMessage) {
        if (errorMessage != null) {
            return new BigDecimal("1.0"); // 执行失败，用户满意度低
        }

        if (executionResult != null && executionResult.contains("success")) {
            return new BigDecimal("4.5"); // 执行成功，用户满意度高
        } else {
            return new BigDecimal("3.0"); // 执行结果不明确
        }
    }

    /**
     * 分析优势
     */
    private List<String> analyzeStrengths(Map<String, Object> executionContext, 
                                         String executionResult, String errorMessage) {
        List<String> strengths = new ArrayList<>();

        if (errorMessage == null) {
            strengths.add("执行成功完成");
        }

        if (executionContext != null && executionContext.containsKey("inputs")) {
            strengths.add("能够正确处理输入参数");
        }

        return strengths;
    }

    /**
     * 分析劣势
     */
    private List<String> analyzeWeaknesses(Map<String, Object> executionContext, 
                                          String executionResult, String errorMessage) {
        List<String> weaknesses = new ArrayList<>();

        if (errorMessage != null) {
            weaknesses.add("执行过程中出现错误: " + errorMessage);
        }

        if (executionResult == null || executionResult.isEmpty()) {
            weaknesses.add("执行结果为空");
        }

        return weaknesses;
    }

    /**
     * 生成评估摘要
     */
    private String generateSummary(Agent agent, String executionResult, String errorMessage, 
                                 BigDecimal performanceScore, BigDecimal accuracyScore, 
                                 BigDecimal efficiencyScore, BigDecimal userSatisfactionScore, 
                                 List<String> strengths, List<String> weaknesses) {
        StringBuilder summary = new StringBuilder();

        summary.append("Agent '").append(agent.getName()).append("' 执行评估摘要:\n");
        summary.append("====================================\n");
        summary.append("执行状态: ").append(errorMessage == null ? "成功" : "失败").append("\n");
        if (errorMessage != null) {
            summary.append("错误信息: " + errorMessage).append("\n");
        }
        summary.append("====================================\n");
        summary.append("评分:\n");
        summary.append("- 性能: " + performanceScore).append("\n");
        summary.append("- 准确性: " + accuracyScore).append("\n");
        summary.append("- 效率: " + efficiencyScore).append("\n");
        summary.append("- 用户满意度: " + userSatisfactionScore).append("\n");
        summary.append("====================================\n");
        summary.append("优势:\n");
        for (String strength : strengths) {
            summary.append("- " + strength).append("\n");
        }
        summary.append("====================================\n");
        summary.append("劣势:\n");
        for (String weakness : weaknesses) {
            summary.append("- " + weakness).append("\n");
        }

        return summary.toString();
    }

    /**
     * 生成改进建议
     */
    private void generateImprovementSuggestions(Agent agent, AgentEvolutionReflection reflection, 
                                              Map<String, Object> executionContext, 
                                              String executionResult, String errorMessage, 
                                              Long tenantId, Long userId) {
        List<AgentEvolutionSuggestion> suggestions = new ArrayList<>();

        // 基于错误信息生成建议
        if (errorMessage != null) {
            AgentEvolutionSuggestion errorSuggestion = new AgentEvolutionSuggestion();
            errorSuggestion.setTenantId(tenantId);
            errorSuggestion.setAgentId(agent.getId());
            errorSuggestion.setReflectionId(reflection.getId());
            errorSuggestion.setSuggestionType("ERROR_FIX");
            errorSuggestion.setTitle("修复执行错误");
            errorSuggestion.setDescription("解决执行过程中出现的错误: " + errorMessage);
            errorSuggestion.setContent("{\"errorMessage\": \"" + errorMessage + "\", \"recommendedAction\": \"检查Agent配置和执行逻辑\"}");
            errorSuggestion.setPriority(1); // 高优先级
            errorSuggestion.setStatus("PENDING");
            errorSuggestion.setImplementationStatus("NOT_IMPLEMENTED");
            errorSuggestion.setExpectedImpact(new BigDecimal("4.0"));
            errorSuggestion.setCreatedBy(userId);
            suggestions.add(errorSuggestion);
        }

        // 基于执行时间生成建议
        if (executionContext != null && executionContext.containsKey("executionTime")) {
            long executionTime = (long) executionContext.get("executionTime");
            if (executionTime > 3000) {
                AgentEvolutionSuggestion performanceSuggestion = new AgentEvolutionSuggestion();
                performanceSuggestion.setTenantId(tenantId);
                performanceSuggestion.setAgentId(agent.getId());
                performanceSuggestion.setReflectionId(reflection.getId());
                performanceSuggestion.setSuggestionType("PERFORMANCE_OPTIMIZATION");
                performanceSuggestion.setTitle("优化执行性能");
                performanceSuggestion.setDescription("执行时间过长，建议优化Agent执行逻辑");
                performanceSuggestion.setContent("{\"currentExecutionTime\": " + executionTime + ", \"recommendedAction\": \"优化执行逻辑，减少不必要的操作\"}");
                performanceSuggestion.setPriority(2); // 中优先级
                performanceSuggestion.setStatus("PENDING");
                performanceSuggestion.setImplementationStatus("NOT_IMPLEMENTED");
                performanceSuggestion.setExpectedImpact(new BigDecimal("3.5"));
                performanceSuggestion.setCreatedBy(userId);
                suggestions.add(performanceSuggestion);
            }
        }

        // 保存建议
        if (!suggestions.isEmpty()) {
            suggestionRepository.saveAll(suggestions);
        }
    }

    /**
     * 获取Agent的反思评估历史
     */
    public List<AgentEvolutionReflection> getAgentReflections(Long agentId, Long tenantId) {
        return reflectionRepository.findByAgentIdAndTenantId(agentId, tenantId);
    }

    /**
     * 获取反思评估的改进建议
     */
    public List<AgentEvolutionSuggestion> getReflectionSuggestions(Long reflectionId) {
        return suggestionRepository.findByReflectionId(reflectionId);
    }

    /**
     * 更新建议的实现状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSuggestionStatus(Long suggestionId, String implementationStatus, Long userId) {
        AgentEvolutionSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        
        suggestion.setImplementationStatus(implementationStatus);
        if ("IMPLEMENTED".equals(implementationStatus)) {
            suggestion.setImplementedBy(userId);
            suggestion.setImplementedAt(LocalDateTime.now());
        }
        
        suggestionRepository.save(suggestion);
    }
}
