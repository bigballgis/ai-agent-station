package com.aiagent.service;

import com.aiagent.entity.AgentEvolutionSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface SuggestionService {
    
    // 生成建议
    List<AgentEvolutionSuggestion> generateSuggestions(Long agentId);
    
    // 存储和管理建议
    AgentEvolutionSuggestion createSuggestion(AgentEvolutionSuggestion suggestion);
    
    AgentEvolutionSuggestion updateSuggestion(Long id, AgentEvolutionSuggestion suggestionDetails);
    
    void deleteSuggestion(Long id);
    
    AgentEvolutionSuggestion getSuggestionById(Long id);
    
    List<AgentEvolutionSuggestion> getAllSuggestions();
    
    // 检索和分析建议
    Page<AgentEvolutionSuggestion> searchSuggestions(String keyword, String suggestionType, String status, Pageable pageable);
    
    List<AgentEvolutionSuggestion> getSuggestionsByAgentId(Long agentId);
    
    List<AgentEvolutionSuggestion> getSuggestionsByType(String suggestionType);
    
    // 优先级排序
    List<AgentEvolutionSuggestion> getSuggestionsByPriority(Long agentId);
    
    // 更新建议状态
    AgentEvolutionSuggestion updateSuggestionStatus(Long id, String status);
    
    // 更新实现状态
    AgentEvolutionSuggestion updateImplementationStatus(Long id, String implementationStatus);
    
    // 分析建议效果
    Map<String, Object> analyzeSuggestionEffectiveness();
}
