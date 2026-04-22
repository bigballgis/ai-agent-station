package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.entity.AgentEvolutionExperience;
import com.aiagent.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    // 创建经验
    @PostMapping
    public Result<AgentEvolutionExperience> createExperience(@RequestBody AgentEvolutionExperience experience) {
        AgentEvolutionExperience createdExperience = experienceService.createExperience(experience);
        return Result.success(createdExperience);
    }

    // 更新经验
    @PutMapping("/{id}")
    public Result<AgentEvolutionExperience> updateExperience(@PathVariable Long id, @RequestBody AgentEvolutionExperience experienceDetails) {
        AgentEvolutionExperience updatedExperience = experienceService.updateExperience(id, experienceDetails);
        return Result.success(updatedExperience);
    }

    // 删除经验
    @DeleteMapping("/{id}")
    public Result<Void> deleteExperience(@PathVariable Long id) {
        experienceService.deleteExperience(id);
        return Result.success();
    }

    // 获取单个经验
    @GetMapping("/{id}")
    public Result<AgentEvolutionExperience> getExperienceById(@PathVariable Long id) {
        AgentEvolutionExperience experience = experienceService.getExperienceById(id);
        return Result.success(experience);
    }

    // 获取所有经验
    @GetMapping
    public Result<List<AgentEvolutionExperience>> getAllExperiences() {
        List<AgentEvolutionExperience> experiences = experienceService.getAllExperiences();
        return Result.success(experiences);
    }

    // 搜索经验
    @GetMapping("/search")
    public Result<Page<AgentEvolutionExperience>> searchExperiences(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String experienceType,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AgentEvolutionExperience> result = experienceService.searchExperiences(keyword, experienceType, tags, pageable);
        return Result.success(result);
    }

    // 按Agent ID获取经验
    @GetMapping("/agent/{agentId}")
    public Result<List<AgentEvolutionExperience>> getExperiencesByAgentId(@PathVariable Long agentId) {
        List<AgentEvolutionExperience> experiences = experienceService.getExperiencesByAgentId(agentId);
        return Result.success(experiences);
    }

    // 按类型获取经验
    @GetMapping("/type/{experienceType}")
    public Result<List<AgentEvolutionExperience>> getExperiencesByType(@PathVariable String experienceType) {
        List<AgentEvolutionExperience> experiences = experienceService.getExperiencesByType(experienceType);
        return Result.success(experiences);
    }

    // 分析经验有效性
    @GetMapping("/analysis/effectiveness")
    public Result<Map<String, Object>> analyzeExperienceEffectiveness() {
        Map<String, Object> analysis = experienceService.analyzeExperienceEffectiveness();
        return Result.success(analysis);
    }

    // 去重经验
    @PostMapping("/deduplicate")
    public Result<Void> deduplicateExperiences() {
        experienceService.deduplicateExperiences();
        return Result.success();
    }

    // 清理过期经验
    @PostMapping("/cleanup")
    public Result<Void> cleanupExpiredExperiences() {
        experienceService.cleanupExpiredExperiences();
        return Result.success();
    }

    // 增加使用次数
    @PostMapping("/{id}/increment-usage")
    public Result<Void> incrementUsageCount(@PathVariable Long id) {
        experienceService.incrementUsageCount(id);
        return Result.success();
    }
}
