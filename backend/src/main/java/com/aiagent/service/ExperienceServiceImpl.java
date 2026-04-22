package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.AgentEvolutionExperience;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.AgentEvolutionExperienceRepository;
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
public class ExperienceServiceImpl implements ExperienceService {

    private final AgentEvolutionExperienceRepository experienceRepository;

    @Override
    @Transactional
    public AgentEvolutionExperience createExperience(AgentEvolutionExperience experience) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = getCurrentUserId();

        if (tenantId != null) {
            experience.setTenantId(tenantId);
        }

        // 检查经验代码是否已存在
        if (experienceRepository.findByExperienceCodeAndTenantId(experience.getExperienceCode(), tenantId).isPresent()) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "经验代码已存在");
        }

        experience.setCreatedBy(userId);
        experience.setUsageCount(0);
        experience.setStatus(1); // 1表示有效

        return experienceRepository.save(experience);
    }

    @Override
    @Transactional
    public AgentEvolutionExperience updateExperience(Long id, AgentEvolutionExperience experienceDetails) {
        AgentEvolutionExperience experience = getExperienceById(id);
        Long userId = getCurrentUserId();

        experience.setTitle(experienceDetails.getTitle());
        experience.setDescription(experienceDetails.getDescription());
        experience.setContent(experienceDetails.getContent());
        experience.setTags(experienceDetails.getTags());
        experience.setEffectivenessScore(experienceDetails.getEffectivenessScore());
        experience.setStatus(experienceDetails.getStatus());
        experience.setUpdatedBy(userId);

        return experienceRepository.save(experience);
    }

    @Override
    @Transactional
    public void deleteExperience(Long id) {
        AgentEvolutionExperience experience = getExperienceById(id);
        experienceRepository.delete(experience);
    }

    @Override
    public AgentEvolutionExperience getExperienceById(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return experienceRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
        }
        return experienceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public List<AgentEvolutionExperience> getAllExperiences() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return experienceRepository.findByTenantId(tenantId);
        }
        return experienceRepository.findAll();
    }

    @Override
    public Page<AgentEvolutionExperience> searchExperiences(String keyword, String experienceType, List<String> tags, Pageable pageable) {
        Long tenantId = TenantContextHolder.getTenantId();

        Specification<AgentEvolutionExperience> spec = (root, query, criteriaBuilder) -> {
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

            // 经验类型过滤
            if (experienceType != null && !experienceType.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("experienceType"), experienceType));
            }

            // 标签过滤
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    predicates.add(criteriaBuilder.isMember(tag, root.get("tags")));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return experienceRepository.findAll(spec, pageable);
    }

    @Override
    public List<AgentEvolutionExperience> getExperiencesByAgentId(Long agentId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return experienceRepository.findByAgentIdAndTenantId(agentId, tenantId);
        }
        // 如果没有租户ID，这里可以根据实际需求调整查询逻辑
        return experienceRepository.findByAgentIdAndTenantId(agentId, null);
    }

    @Override
    public List<AgentEvolutionExperience> getExperiencesByType(String experienceType) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return experienceRepository.findByExperienceTypeAndTenantId(experienceType, tenantId);
        }
        // 如果没有租户ID，这里可以根据实际需求调整查询逻辑
        return experienceRepository.findByExperienceTypeAndTenantId(experienceType, null);
    }

    @Override
    public Map<String, Object> analyzeExperienceEffectiveness() {
        Long tenantId = TenantContextHolder.getTenantId();
        List<AgentEvolutionExperience> experiences;

        if (tenantId != null) {
            experiences = experienceRepository.findByTenantId(tenantId);
        } else {
            experiences = experienceRepository.findAll();
        }

        Map<String, Object> analysis = new HashMap<>();

        // 计算平均有效性得分
        Optional<BigDecimal> avgScore = experiences.stream()
                .filter(exp -> exp.getEffectivenessScore() != null)
                .map(AgentEvolutionExperience::getEffectivenessScore)
                .reduce(BigDecimal::add);

        if (avgScore.isPresent()) {
            int count = (int) experiences.stream().filter(exp -> exp.getEffectivenessScore() != null).count();
            analysis.put("averageEffectivenessScore", avgScore.get().divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP));
        }

        // 按经验类型分组分析
        Map<String, List<AgentEvolutionExperience>> experiencesByType = experiences.stream()
                .collect(Collectors.groupingBy(AgentEvolutionExperience::getExperienceType));

        Map<String, Object> typeAnalysis = new HashMap<>();
        for (Map.Entry<String, List<AgentEvolutionExperience>> entry : experiencesByType.entrySet()) {
            List<AgentEvolutionExperience> typeExperiences = entry.getValue();
            Map<String, Object> typeStats = new HashMap<>();
            typeStats.put("count", typeExperiences.size());
            
            Optional<BigDecimal> typeAvgScore = typeExperiences.stream()
                    .filter(exp -> exp.getEffectivenessScore() != null)
                    .map(AgentEvolutionExperience::getEffectivenessScore)
                    .reduce(BigDecimal::add);
            
            if (typeAvgScore.isPresent()) {
                int typeCount = (int) typeExperiences.stream().filter(exp -> exp.getEffectivenessScore() != null).count();
                typeStats.put("averageEffectivenessScore", typeAvgScore.get().divide(BigDecimal.valueOf(typeCount), 2, BigDecimal.ROUND_HALF_UP));
            }
            
            typeAnalysis.put(entry.getKey(), typeStats);
        }
        analysis.put("byType", typeAnalysis);

        // 计算总使用次数
        int totalUsage = experiences.stream().mapToInt(AgentEvolutionExperience::getUsageCount).sum();
        analysis.put("totalUsageCount", totalUsage);

        return analysis;
    }

    @Override
    @Transactional
    public void deduplicateExperiences() {
        Long tenantId = TenantContextHolder.getTenantId();
        List<AgentEvolutionExperience> experiences;

        if (tenantId != null) {
            experiences = experienceRepository.findByTenantId(tenantId);
        } else {
            experiences = experienceRepository.findAll();
        }

        // 按标题和内容分组，找出重复的经验
        Map<String, List<AgentEvolutionExperience>> duplicateGroups = experiences.stream()
                .collect(Collectors.groupingBy(exp -> exp.getTitle() + "_" + exp.getContent()));

        for (Map.Entry<String, List<AgentEvolutionExperience>> entry : duplicateGroups.entrySet()) {
            List<AgentEvolutionExperience> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                // 保留最早创建的经验，删除其他重复的
                duplicates.sort(Comparator.comparing(AgentEvolutionExperience::getCreatedAt));
                AgentEvolutionExperience retainedExperience = duplicates.get(0);
                
                // 合并标签
                Set<String> allTags = new HashSet<>();
                for (AgentEvolutionExperience exp : duplicates) {
                    if (exp.getTags() != null) {
                        allTags.addAll(exp.getTags());
                    }
                }
                retainedExperience.setTags(new ArrayList<>(allTags));
                
                // 累加使用次数
                int totalUsage = duplicates.stream().mapToInt(AgentEvolutionExperience::getUsageCount).sum();
                retainedExperience.setUsageCount(totalUsage);
                
                // 保存更新后的经验
                experienceRepository.save(retainedExperience);
                
                // 删除其他重复的经验
                for (int i = 1; i < duplicates.size(); i++) {
                    experienceRepository.delete(duplicates.get(i));
                }
            }
        }
    }

    @Override
    @Transactional
    public void cleanupExpiredExperiences() {
        Long tenantId = TenantContextHolder.getTenantId();
        List<AgentEvolutionExperience> experiences;

        if (tenantId != null) {
            experiences = experienceRepository.findByTenantId(tenantId);
        } else {
            experiences = experienceRepository.findAll();
        }

        // 清理状态为无效的经验
        List<AgentEvolutionExperience> expiredExperiences = experiences.stream()
                .filter(exp -> exp.getStatus() == 0) // 0表示无效
                .collect(Collectors.toList());

        for (AgentEvolutionExperience experience : expiredExperiences) {
            experienceRepository.delete(experience);
        }
    }

    @Override
    @Transactional
    public void incrementUsageCount(Long id) {
        AgentEvolutionExperience experience = getExperienceById(id);
        experience.setUsageCount(experience.getUsageCount() + 1);
        experienceRepository.save(experience);
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getId();
        }
        return null;
    }
}
