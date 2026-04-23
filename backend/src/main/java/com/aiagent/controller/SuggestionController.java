package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.CreateSuggestionRequestDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.SuggestionResponseDTO;
import com.aiagent.dto.UpdateSuggestionRequestDTO;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/v1/suggestions")
@RequiredArgsConstructor
@Tag(name = "建议管理", description = "Agent建议管理接口")
public class SuggestionController {

    private final SuggestionService suggestionService;

    // 生成建议
    @RequiresPermission("suggestion:manage")
    @PostMapping("/generate/{agentId}")
    @Operation(summary = "生成Agent优化建议")
    public Result<List<SuggestionResponseDTO>> generateSuggestions(@PathVariable Long agentId) {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.generateSuggestions(agentId);
        return Result.success(suggestions.stream().map(DTOConverter::toSuggestionResponseDTO).toList());
    }

    // 创建建议
    @RequiresPermission("suggestion:manage")
    @Operation(summary = "创建建议")
    @PostMapping
    public Result<SuggestionResponseDTO> createSuggestion(@RequestBody CreateSuggestionRequestDTO requestDTO) {
        AgentEvolutionSuggestion suggestion = DTOConverter.toSuggestionEntity(requestDTO);
        AgentEvolutionSuggestion createdSuggestion = suggestionService.createSuggestion(suggestion);
        return Result.success(DTOConverter.toSuggestionResponseDTO(createdSuggestion));
    }

    // 更新建议
    @RequiresPermission("suggestion:manage")
    @Operation(summary = "更新建议")
    @PutMapping("/{id}")
    public Result<SuggestionResponseDTO> updateSuggestion(@PathVariable Long id, @RequestBody UpdateSuggestionRequestDTO requestDTO) {
        AgentEvolutionSuggestion existing = suggestionService.getSuggestionById(id);
        DTOConverter.updateSuggestionFromDTO(requestDTO, existing);
        AgentEvolutionSuggestion updatedSuggestion = suggestionService.updateSuggestion(id, existing);
        return Result.success(DTOConverter.toSuggestionResponseDTO(updatedSuggestion));
    }

    // 删除建议
    @RequiresPermission("suggestion:manage")
    @Operation(summary = "删除建议")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSuggestion(@PathVariable Long id) {
        suggestionService.deleteSuggestion(id);
        return Result.success();
    }

    // 获取单个建议
    @RequiresPermission("suggestion:view")
    @Operation(summary = "根据ID获取建议详情")
    @GetMapping("/{id}")
    public Result<SuggestionResponseDTO> getSuggestionById(@PathVariable Long id) {
        AgentEvolutionSuggestion suggestion = suggestionService.getSuggestionById(id);
        return Result.success(DTOConverter.toSuggestionResponseDTO(suggestion));
    }

    // 获取所有建议
    @RequiresPermission("suggestion:view")
    @Operation(summary = "获取所有建议列表")
    @GetMapping
    public Result<List<SuggestionResponseDTO>> getAllSuggestions() {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.getAllSuggestions();
        return Result.success(suggestions.stream().map(DTOConverter::toSuggestionResponseDTO).toList());
    }

    // 搜索建议
    @RequiresPermission("suggestion:view")
    @Operation(summary = "搜索建议")
    @GetMapping("/search")
    public Result<PageResult<SuggestionResponseDTO>> searchSuggestions(
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
        Page<SuggestionResponseDTO> dtoPage = result.map(DTOConverter::toSuggestionResponseDTO);
        return Result.success(PageResult.from(dtoPage));
    }

    // 按Agent ID获取建议
    @RequiresPermission("suggestion:view")
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "根据Agent ID获取建议列表")
    public Result<List<SuggestionResponseDTO>> getSuggestionsByAgentId(@PathVariable Long agentId) {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.getSuggestionsByAgentId(agentId);
        return Result.success(suggestions.stream().map(DTOConverter::toSuggestionResponseDTO).toList());
    }

    // 按类型获取建议
    @RequiresPermission("suggestion:view")
    @Operation(summary = "根据类型获取建议列表")
    @GetMapping("/type/{suggestionType}")
    public Result<List<SuggestionResponseDTO>> getSuggestionsByType(@PathVariable String suggestionType) {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.getSuggestionsByType(suggestionType);
        return Result.success(suggestions.stream().map(DTOConverter::toSuggestionResponseDTO).toList());
    }

    // 按优先级获取建议
    @RequiresPermission("suggestion:view")
    @Operation(summary = "根据优先级获取建议列表")
    @GetMapping("/priority/{agentId}")
    public Result<List<SuggestionResponseDTO>> getSuggestionsByPriority(@PathVariable Long agentId) {
        List<AgentEvolutionSuggestion> suggestions = suggestionService.getSuggestionsByPriority(agentId);
        return Result.success(suggestions.stream().map(DTOConverter::toSuggestionResponseDTO).toList());
    }

    // 更新建议状态
    @RequiresPermission("suggestion:manage")
    @PutMapping("/{id}/status")
    @Operation(summary = "更新建议状态")
    public Result<SuggestionResponseDTO> updateSuggestionStatus(@PathVariable Long id, @RequestParam String status) {
        AgentEvolutionSuggestion suggestion = suggestionService.updateSuggestionStatus(id, status);
        return Result.success(DTOConverter.toSuggestionResponseDTO(suggestion));
    }

    // 更新实现状态
    @RequiresPermission("suggestion:manage")
    @PutMapping("/{id}/implementation-status")
    @Operation(summary = "更新实现状态")
    public Result<SuggestionResponseDTO> updateImplementationStatus(@PathVariable Long id, @RequestParam String implementationStatus) {
        AgentEvolutionSuggestion suggestion = suggestionService.updateImplementationStatus(id, implementationStatus);
        return Result.success(DTOConverter.toSuggestionResponseDTO(suggestion));
    }

    // 分析建议效果
    @RequiresPermission("suggestion:view")
    @Operation(summary = "分析建议效果")
    @GetMapping("/analysis/effectiveness")
    public Result<Map<String, Object>> analyzeSuggestionEffectiveness() {
        Map<String, Object> analysis = suggestionService.analyzeSuggestionEffectiveness();
        return Result.success(analysis);
    }
}
