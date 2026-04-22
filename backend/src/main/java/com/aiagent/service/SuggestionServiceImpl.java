package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.AgentEvolutionExperience;
import com.aiagent.entity.AgentEvolutionSuggestion;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentEvolutionExperienceRepository;
import com.aiagent.repository.AgentEvolutionSuggestionRepository;
import com.aiagent.security.UserPrincipal;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {

    private final AgentEvolutionSuggestionRepository suggestionRepository;
    private final AgentEvolutionExperienceRepository experienceRepository;

    @Override
    @Transactional
    public List<AgentEvolutionSuggestion> generateSuggestions(Long agentId) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = getCurrentUserId();
        List<AgentEvolutionSuggestion> suggestions = new ArrayList<>();

        // 基于经验数据生成建议
        List<AgentEvolutionExperience> experiences = experienceRepository.findByAgentIdAndTenantId(agentId, tenantId);

        // 分析经验数据，生成优化建议
        Map<String, List<AgentEvolutionExperience>> experiencesByType = experiences.stream()
                .collect(Collectors.groupingBy(AgentEvolutionExperience::getExperienceType));

        // 生成流程优化建议
        if (experiencesByType.containsKey("process")) {
            AgentEvolutionSuggestion processSuggestion = createProcessOptimizationSuggestion(agentId, tenantId, userId, experiencesByType.get("process"));
            suggestions.add(processSuggestion);
        }

        // 生成性能优化建议
        if (experiencesByType.containsKey("performance")) {
            AgentEvolutionSuggestion performanceSuggestion = createPerformanceOptimizationSuggestion(agentId, tenantId, userId, experiencesByType.get("performance"));
            suggestions.add(performanceSuggestion);
        }

        // 生成质量优化建议
        if (experiencesByType.containsKey("quality")) {
            AgentEvolutionSuggestion qualitySuggestion = createQualityOptimizationSuggestion(agentId, tenantId, userId, experiencesByType.get("quality"));
            suggestions.add(qualitySuggestion);
        }

        // 保存生成的建议
        for (AgentEvolutionSuggestion suggestion : suggestions) {
            suggestionRepository.save(suggestion);
        }

        return suggestions;
    }

    private AgentEvolutionSuggestion createProcessOptimizationSuggestion(Long agentId, Long tenantId, Long userId, List<AgentEvolutionExperience> processExperiences) {
        AgentEvolutionSuggestion suggestion = new AgentEvolutionSuggestion();
        suggestion.setTenantId(tenantId);
        suggestion.setAgentId(agentId);
        suggestion.setSuggestionType("process");
        suggestion.setTitle("流程优化建议");
        suggestion.setDescription("基于历史流程经验数据生成的优化建议");
        
        // 分析流程经验，生成具体建议内容
        StringBuilder content = new StringBuilder();
        content.append("{\n");
        content.append("  \"recommendations\": [\n");
        
        // 统计流程步骤
        Map<String, Integer> stepCount = new HashMap<>();
        for (AgentEvolutionExperience exp : processExperiences) {
            if (exp.getContent() != null) {
                // 简单解析内容，提取流程步骤
                // 实际项目中可能需要更复杂的解析逻辑
                stepCount.merge("步骤", 1, Integer::sum);
            }
        }
        
        content.append("    {\"action\": \"简化流程步骤\", \"reason\": \"基于\" + stepCount.getOrDefault(\"步骤\", 0) + \"次流程执行经验\"},");
        content.append("    {\"action\": \"优化决策节点\", \"reason\": \"减少不必要的审批环节\"}");
        
        content.append("  ]\n");
        content.append("}");
        
        suggestion.setContent(content.toString());
        suggestion.setPriority(calculatePriority(processExperiences));
        suggestion.setStatus("pending");
        suggestion.setImplementationStatus("not_implemented");
        suggestion.setExpectedImpact(new BigDecimal("0.8"));
        suggestion.setCreatedBy(userId);
        
        return suggestion;
    }

    private AgentEvolutionSuggestion createPerformanceOptimizationSuggestion(Long agentId, Long tenantId, Long userId, List<AgentEvolutionExperience> performanceExperiences) {
        AgentEvolutionSuggestion suggestion = new AgentEvolutionSuggestion();
        suggestion.setTenantId(tenantId);
        suggestion.setAgentId(agentId);
        suggestion.setSuggestionType("performance");
        suggestion.setTitle("性能优化建议");
        suggestion.setDescription("基于历史性能数据生成的优化建议");
        
        // 分析性能经验，生成具体建议内容
        StringBuilder content = new StringBuilder();
        content.append("{\n");
        content.append("  \"recommendations\": [\n");
        content.append("    {\"action\": \"优化算法\", \"reason\": \"提升处理速度\"},");
        content.append("    {\"action\": \"增加缓存\", \"reason\": \"减少重复计算\"}");
        content.append("  ]\n");
        content.append("}");
        
        suggestion.setContent(content.toString());
        suggestion.setPriority(calculatePriority(performanceExperiences));
        suggestion.setStatus("pending");
        suggestion.setImplementationStatus("not_implemented");
        suggestion.setExpectedImpact(new BigDecimal("0.9"));
        suggestion.setCreatedBy(userId);
        
        return suggestion;
    }

    private AgentEvolutionSuggestion createQualityOptimizationSuggestion(Long agentId, Long tenantId, Long userId, List<AgentEvolutionExperience> qualityExperiences) {
        AgentEvolutionSuggestion suggestion = new AgentEvolutionSuggestion();
        suggestion.setTenantId(tenantId);
        suggestion.setAgentId(agentId);
        suggestion.setSuggestionType("quality");
        suggestion.setTitle("质量优化建议");
        suggestion.setDescription("基于历史质量数据生成的优化建议");
        
        // 分析质量经验，生成具体建议内容
        StringBuilder content = new StringBuilder();
        content.append("{\n");
        content.append("  \"recommendations\": [\n");
        content.append("    {\"action\": \"增加测试用例\", \"reason\": \"提高代码覆盖率\"},");
        content.append("    {\"action\": \"优化错误处理\", \"reason\": \"提升系统稳定性\"}");
        content.append("  ]\n");
        content.append("}");
        
        suggestion.setContent(content.toString());
        suggestion.setPriority(calculatePriority(qualityExperiences));
        suggestion.setStatus("pending");
        suggestion.setImplementationStatus("not_implemented");
        suggestion.setExpectedImpact(new BigDecimal("0.7"));
        suggestion.setCreatedBy(userId);
        
        return suggestion;
    }

    private int calculatePriority(List<AgentEvolutionExperience> experiences) {
        // 基于经验数据计算优先级
        if (experiences.isEmpty()) {
            return 3; // 默认中等优先级
        }
        
        // 计算平均有效性得分
        Optional<BigDecimal> avgScore = experiences.stream()
                .filter(exp -> exp.getEffectivenessScore() != null)
                .map(AgentEvolutionExperience::getEffectivenessScore)
                .reduce(BigDecimal::add);
        
        if (avgScore.isPresent()) {
            int count = (int) experiences.stream().filter(exp -> exp.getEffectivenessScore() != null).count();
            BigDecimal average = avgScore.get().divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);
            
            if (average.compareTo(new BigDecimal("0.8")) >= 0) {
                return 1; // 高优先级
            } else if (average.compareTo(new BigDecimal("0.5")) >= 0) {
                return 2; // 中优先级
            } else {
                return 3; // 低优先级
            }
        }
        
        return 3; // 默认中等优先级
    }

    @Override
    @Transactional
    public AgentEvolutionSuggestion createSuggestion(AgentEvolutionSuggestion suggestion) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = getCurrentUserId();

        if (tenantId != null) {
            suggestion.setTenantId(tenantId);
        }

        suggestion.setCreatedBy(userId);
        suggestion.setStatus("pending");
        suggestion.setImplementationStatus("not_implemented");

        return suggestionRepository.save(suggestion);
    }

    @Override
    @Transactional
    public AgentEvolutionSuggestion updateSuggestion(Long id, AgentEvolutionSuggestion suggestionDetails) {
        AgentEvolutionSuggestion suggestion = getSuggestionById(id);
        Long userId = getCurrentUserId();

        suggestion.setTitle(suggestionDetails.getTitle());
        suggestion.setDescription(suggestionDetails.getDescription());
        suggestion.setContent(suggestionDetails.getContent());
        suggestion.setPriority(suggestionDetails.getPriority());
        suggestion.setStatus(suggestionDetails.getStatus());
        suggestion.setImplementationStatus(suggestionDetails.getImplementationStatus());
        suggestion.setExpectedImpact(suggestionDetails.getExpectedImpact());
        suggestion.setActualImpact(suggestionDetails.getActualImpact());
        suggestion.setUpdatedBy(userId);

        return suggestionRepository.save(suggestion);
    }

    @Override
    @Transactional
    public void deleteSuggestion(Long id) {
        AgentEvolutionSuggestion suggestion = getSuggestionById(id);
        suggestionRepository.delete(suggestion);
    }

    @Override
    public AgentEvolutionSuggestion getSuggestionById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return suggestionRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
        }
        return suggestionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public List<AgentEvolutionSuggestion> getAllSuggestions() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return suggestionRepository.findByTenantId(tenantId);
        }
        return suggestionRepository.findAll();
    }

    @Override
    public Page<AgentEvolutionSuggestion> searchSuggestions(String keyword, String suggestionType, String status, Pageable pageable) {
        Long tenantId = TenantContextHolder.getTenantId();

        Specification<AgentEvolutionSuggestion> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 租户隔离
            if (tenantId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            }

            // 关键词搜索
            if (keyword != null && !keyword.isEmpty()) {
                String likeKeyword = "%" + keyword + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("title"), likeKeyword),
                        criteriaBuilder.like(root.get("description"), likeKeyword)
                ));
            }

            // 建议类型过滤
            if (suggestionType != null && !suggestionType.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("suggestionType"), suggestionType));
            }

            // 状态过滤
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return suggestionRepository.findAll(spec, pageable);
    }

    @Override
    public List<AgentEvolutionSuggestion> getSuggestionsByAgentId(Long agentId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return suggestionRepository.findByAgentIdAndTenantId(agentId, tenantId);
        }
        return suggestionRepository.findByAgentIdAndTenantId(agentId, null);
    }

    @Override
    public List<AgentEvolutionSuggestion> getSuggestionsByType(String suggestionType) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return suggestionRepository.findBySuggestionTypeAndTenantId(suggestionType, tenantId);
        }
        return suggestionRepository.findBySuggestionTypeAndTenantId(suggestionType, null);
    }

    @Override
    public List<AgentEvolutionSuggestion> getSuggestionsByPriority(Long agentId) {
        Long tenantId = TenantContextHolder.getTenantId();
        List<AgentEvolutionSuggestion> suggestions;

        if (tenantId != null) {
            suggestions = suggestionRepository.findByAgentIdAndTenantId(agentId, tenantId);
        } else {
            suggestions = suggestionRepository.findByAgentIdAndTenantId(agentId, null);
        }

        // 按优先级排序，1最高，3最低
        suggestions.sort(Comparator.comparing(AgentEvolutionSuggestion::getPriority));

        return suggestions;
    }

    @Override
    @Transactional
    public AgentEvolutionSuggestion updateSuggestionStatus(Long id, String status) {
        AgentEvolutionSuggestion suggestion = getSuggestionById(id);
        Long userId = getCurrentUserId();

        suggestion.setStatus(status);
        suggestion.setUpdatedBy(userId);

        return suggestionRepository.save(suggestion);
    }

    @Override
    @Transactional
    public AgentEvolutionSuggestion updateImplementationStatus(Long id, String implementationStatus) {
        AgentEvolutionSuggestion suggestion = getSuggestionById(id);
        Long userId = getCurrentUserId();

        suggestion.setImplementationStatus(implementationStatus);
        if ("implemented".equals(implementationStatus)) {
            suggestion.setImplementedBy(userId);
            suggestion.setImplementedAt(LocalDateTime.now());
        }
        suggestion.setUpdatedBy(userId);

        return suggestionRepository.save(suggestion);
    }

    @Override
    public Map<String, Object> analyzeSuggestionEffectiveness() {
        Long tenantId = TenantContextHolder.getTenantId();
        List<AgentEvolutionSuggestion> suggestions;

        if (tenantId != null) {
            suggestions = suggestionRepository.findByTenantId(tenantId);
        } else {
            suggestions = suggestionRepository.findAll();
        }

        Map<String, Object> analysis = new HashMap<>();

        // 计算平均预期影响
        Optional<BigDecimal> avgExpectedImpact = suggestions.stream()
                .filter(s -> s.getExpectedImpact() != null)
                .map(AgentEvolutionSuggestion::getExpectedImpact)
                .reduce(BigDecimal::add);

        if (avgExpectedImpact.isPresent()) {
            int count = (int) suggestions.stream().filter(s -> s.getExpectedImpact() != null).count();
            analysis.put("averageExpectedImpact", avgExpectedImpact.get().divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP));
        }

        // 计算平均实际影响
        Optional<BigDecimal> avgActualImpact = suggestions.stream()
                .filter(s -> s.getActualImpact() != null)
                .map(AgentEvolutionSuggestion::getActualImpact)
                .reduce(BigDecimal::add);

        if (avgActualImpact.isPresent()) {
            int count = (int) suggestions.stream().filter(s -> s.getActualImpact() != null).count();
            analysis.put("averageActualImpact", avgActualImpact.get().divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP));
        }

        // 按建议类型分组分析
        Map<String, List<AgentEvolutionSuggestion>> suggestionsByType = suggestions.stream()
                .collect(Collectors.groupingBy(AgentEvolutionSuggestion::getSuggestionType));

        Map<String, Object> typeAnalysis = new HashMap<>();
        for (Map.Entry<String, List<AgentEvolutionSuggestion>> entry : suggestionsByType.entrySet()) {
            List<AgentEvolutionSuggestion> typeSuggestions = entry.getValue();
            Map<String, Object> typeStats = new HashMap<>();
            typeStats.put("count", typeSuggestions.size());
            
            Optional<BigDecimal> typeAvgExpectedImpact = typeSuggestions.stream()
                    .filter(s -> s.getExpectedImpact() != null)
                    .map(AgentEvolutionSuggestion::getExpectedImpact)
                    .reduce(BigDecimal::add);
            
            if (typeAvgExpectedImpact.isPresent()) {
                int typeCount = (int) typeSuggestions.stream().filter(s -> s.getExpectedImpact() != null).count();
                typeStats.put("averageExpectedImpact", typeAvgExpectedImpact.get().divide(BigDecimal.valueOf(typeCount), 2, BigDecimal.ROUND_HALF_UP));
            }
            
            typeAnalysis.put(entry.getKey(), typeStats);
        }
        analysis.put("byType", typeAnalysis);

        // 按状态分组分析
        Map<String, Long> statusCount = suggestions.stream()
                .collect(Collectors.groupingBy(AgentEvolutionSuggestion::getStatus, Collectors.counting()));
        analysis.put("statusCount", statusCount);

        // 按实现状态分组分析
        Map<String, Long> implementationStatusCount = suggestions.stream()
                .collect(Collectors.groupingBy(AgentEvolutionSuggestion::getImplementationStatus, Collectors.counting()));
        analysis.put("implementationStatusCount", implementationStatusCount);

        return analysis;
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getId();
        }
        return null;
    }
}
