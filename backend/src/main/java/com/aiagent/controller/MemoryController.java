package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.CreateMemoryRequestDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.MemoryResponseDTO;
import com.aiagent.entity.AgentMemory;
import com.aiagent.service.MemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/memories")
@RequiredArgsConstructor
@Tag(name = "记忆管理")
public class MemoryController {

    private final MemoryService memoryService;

    @RequiresPermission("memory:manage")
    @PostMapping
    @Operation(summary = "创建Agent记忆")
    public Result<MemoryResponseDTO> createMemory(@Valid @RequestBody CreateMemoryRequestDTO requestDTO) {
        AgentMemory memory = DTOConverter.toMemoryEntity(requestDTO);
        return Result.created(DTOConverter.toMemoryResponseDTO(memoryService.createMemory(memory)));
    }

    @RequiresPermission("memory:view")
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "获取Agent记忆列表")
    public Result<PageResult<MemoryResponseDTO>> getMemories(
            @Parameter(description = "Agent ID") @PathVariable Long agentId,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword,
            @RequestParam(required = false) @Parameter(description = "记忆类型") String memoryType,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        AgentMemory.MemoryType type = memoryType != null ? AgentMemory.MemoryType.valueOf(memoryType) : null;
        Page<AgentMemory> result = memoryService.getMemories(agentId, keyword, type, page, size);
        Page<MemoryResponseDTO> dtoPage = result.map(DTOConverter::toMemoryResponseDTO);
        return Result.success(PageResult.from(dtoPage));
    }

    @RequiresPermission("memory:view")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取记忆详情")
    public Result<MemoryResponseDTO> getMemory(@Parameter(description = "记忆ID") @PathVariable Long id) {
        return Result.success(DTOConverter.toMemoryResponseDTO(memoryService.getMemory(id)));
    }

    @RequiresPermission("memory:manage")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除记忆")
    public Result<Void> deleteMemory(@Parameter(description = "记忆ID") @PathVariable Long id) {
        memoryService.deleteMemory(id);
        return Result.deleted();
    }

    @RequiresPermission("memory:manage")
    @DeleteMapping("/agent/{agentId}/cleanup")
    @Operation(summary = "清理过期记忆")
    public Result<Void> cleanupExpired(@Parameter(description = "Agent ID") @PathVariable Long agentId) {
        memoryService.cleanupExpiredMemories();
        return Result.success();
    }
}
