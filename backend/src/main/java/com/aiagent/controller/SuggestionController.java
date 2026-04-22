package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.entity.AgentEvolutionSuggestion;
import com.aiagent.service.SuggestionService;
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
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    // 生成建议
    @PostMapping("/generate/{agentId}")
    public Result<List<AgentEvolutionSuggestion>> generateSuggestions(@PathVariable Long agentId) {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.generateSuggestions(agentId);
        return Result.success(suggestions);
    }

    // 创建建议
    @PostMapping
    public Result<AgentEvolutionSuggestion> createSuggestion(@RequestBody AgentEvolutionSuggestion suggestion) {
        AgentEvolutionSuggestion createdSuggestion = suggestionService.createSuggestion(suggestion);
        return Result.success(createdSuggestion);
    }

    // 更新建议
    @PutMapping("/{id}")
    public Result<AgentEvolutionSuggestion> updateSuggestion(@PathVariable Long id, @RequestBody AgentEvolutionSuggestion suggestionDetails) {
        AgentEvolutionSuggestion updatedSuggestion = suggestionService.updateSuggestion(id, suggestionDetails);
        return Result.success(updatedSuggestion);
    }

    // 删除建议
    @DeleteMapping("/{id}")
    public Result<Void> deleteSuggestion(@PathVariable Long id) {
        suggestionService.deleteSuggestion(id);
        return Result.success();
    }

    // 获取单个建议
    @GetMapping("/{id}")
    public Result<AgentEvolutionSuggestion> getSuggestionById(@PathVariable Long id) {
        AgentEvolutionSuggestion suggestion = suggestionService.getSuggestionById(id);
        return Result.success(suggestion);
    }

    // 获取所有建议
    @GetMapping
    public Result<List<AgentEvolutionSuggestion>> getAllSuggestions() {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.getAllSuggestions();
        return Result.success(suggestions);
    }

    // 搜索建议
    @GetMapping("/search")
    public Result<Page<AgentEvolutionSuggestion>> searchSuggestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String suggestionType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AgentEvolutionSuggestion> result = suggestionService.searchSuggestions(keyword, suggestionType, status, pageable);
        return Result.success(result);
    }

    // 按Agent ID获取建议
    @GetMapping("/agent/{agentId}")
    public Result<List<AgentEvolutionSuggestion>> getSuggestionsByAgentId(@PathVariable Long agentId) {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.getSuggestionsByAgentId(agentId);
        return Result.success(suggestions);
    }

    // 按类型获取建议
    @GetMapping("/type/{suggestionType}")
    public Result<List<AgentEvolutionSuggestion>> getSuggestionsByType(@PathVariable String suggestionType) {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.getSuggestionsByType(suggestionType);
        return Result.success(suggestions);
    }

    // 按优先级获取建议
    @GetMapping("/priority/{agentId}")
    public Result<List<AgentEvolutionSuggestion>> getSuggestionsByPriority(@PathVariable Long agentId) {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.getSuggestionsByPriority(agentId);
        return Result.success(suggestions);
    }

    // 更新建议状态
    @PutMapping("/{id}/status")
    public Result<AgentEvolutionSuggestion> updateSuggestionStatus(@PathVariable Long id, @RequestParam String status) {
        AgentEvolutionSuggestion suggestion = suggestionService.updateSuggestionStatus(id, status);
        return Result.success(suggestion);
    }

    // 更新实现状态
    @PutMapping("/{id}/implementation-status")
    public Result<AgentEvolutionSuggestion> updateImplementationStatus(@PathVariable Long id, @RequestParam String implementationStatus) {
        AgentEvolutionSuggestion suggestion = suggestionService.updateImplementationStatus(id, implementationStatus);
        return Result.success(suggestion);
    }

    // 分析建议效果
    @GetMapping("/analysis/effectiveness")
    public Result<Map<String, Object>> analyzeSuggestionEffectiveness() {
        Map<String, Object> analysis = suggestionService.analyzeSuggestionEffectiveness();
        return Result.success(analysis);
    }
}
