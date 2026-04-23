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
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public Result<MemoryResponseDTO> createMemory(@RequestBody CreateMemoryRequestDTO requestDTO) {
        AgentMemory memory = DTOConverter.toMemoryEntity(requestDTO);
        return Result.success(DTOConverter.toMemoryResponseDTO(memoryService.createMemory(memory)));
    }

    @RequiresPermission("memory:view")
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "获取Agent记忆列表")
    public Result<PageResult<MemoryResponseDTO>> getMemories(
            @PathVariable Long agentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String memoryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AgentMemory.MemoryType type = memoryType != null ? AgentMemory.MemoryType.valueOf(memoryType) : null;
        Page<AgentMemory> result = memoryService.getMemories(agentId, keyword, type, page, size);
        Page<MemoryResponseDTO> dtoPage = result.map(DTOConverter::toMemoryResponseDTO);
        return Result.success(PageResult.from(dtoPage));
    }

    @RequiresPermission("memory:view")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取记忆详情")
    public Result<MemoryResponseDTO> getMemory(@PathVariable Long id) {
        return Result.success(DTOConverter.toMemoryResponseDTO(memoryService.getMemory(id)));
    }

    @RequiresPermission("memory:manage")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除记忆")
    public Result<Void> deleteMemory(@PathVariable Long id) {
        memoryService.deleteMemory(id);
        return Result.success();
    }

    @RequiresPermission("memory:manage")
    @DeleteMapping("/agent/{agentId}/cleanup")
    @Operation(summary = "清理过期记忆")
    public Result<Void> cleanupExpired(@PathVariable Long agentId) {
        memoryService.cleanupExpiredMemories();
        return Result.success();
    }
}
