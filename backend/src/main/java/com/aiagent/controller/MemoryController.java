package com.aiagent.controller;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.AgentMemory;
import com.aiagent.entity.AgentMemory.MemoryType;
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

    @PostMapping
    @Operation(summary = "创建记忆")
    public Result<AgentMemory> createMemory(@RequestBody AgentMemory memory) {
        return Result.success(memoryService.createMemory(memory));
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "获取Agent记忆列表")
    public Result<PageResult<AgentMemory>> getMemories(
            @PathVariable Long agentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MemoryType memoryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AgentMemory> result = memoryService.getMemories(agentId, keyword, memoryType, page, size);
        return Result.success(PageResult.from(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取记忆详情")
    public Result<AgentMemory> getMemory(@PathVariable Long id) {
        return Result.success(memoryService.getMemory(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除记忆")
    public Result<Void> deleteMemory(@PathVariable Long id) {
        memoryService.deleteMemory(id);
        return Result.success();
    }

    @DeleteMapping("/agent/{agentId}/cleanup")
    @Operation(summary = "清理过期记忆")
    public Result<Void> cleanupExpired(@PathVariable Long agentId) {
        memoryService.cleanupExpiredMemories();
        return Result.success();
    }
}
