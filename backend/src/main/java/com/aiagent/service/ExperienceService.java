package com.aiagent.service;

import com.aiagent.entity.AgentEvolutionExperience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ExperienceService {
    
    // 存储和管理经验数据
    AgentEvolutionExperience createExperience(AgentEvolutionExperience experience);
    
    AgentEvolutionExperience updateExperience(Long id, AgentEvolutionExperience experienceDetails);
    
    void deleteExperience(Long id);
    
    AgentEvolutionExperience getExperienceById(Long id);
    
    List<AgentEvolutionExperience> getAllExperiences();
    
    // 检索和分析经验数据
    Page<AgentEvolutionExperience> searchExperiences(String keyword, String experienceType, List<String> tags, Pageable pageable);
    
    List<AgentEvolutionExperience> getExperiencesByAgentId(Long agentId);
    
    List<AgentEvolutionExperience> getExperiencesByType(String experienceType);
    
    Map<String, Object> analyzeExperienceEffectiveness();
    
    // 去重和清理经验数据
    void deduplicateExperiences();
    
    void cleanupExpiredExperiences();
    
    void incrementUsageCount(Long id);
}
