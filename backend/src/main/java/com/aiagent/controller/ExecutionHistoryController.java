package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.ExecutionHistoryResponseDTO;
import com.aiagent.service.ExecutionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/execution-history")
@RequiredArgsConstructor
@Tag(name = "执行历史", description = "SSE执行历史管理接口")
public class ExecutionHistoryController {

    private final ExecutionHistoryService executionHistoryService;

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "获取Agent执行历史")
    @RequiresPermission("agent:read")
    public Result<PageResult<ExecutionHistoryResponseDTO>> getHistory(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageResult<ExecutionHistoryResponseDTO> result = executionHistoryService
                .getHistoryByAgentId(agentId, page, size);
        return Result.success(result);
    }

    @DeleteMapping("/agent/{agentId}")
    @Operation(summary = "清除Agent执行历史")
    @RequiresPermission("agent:write")
    public Result<?> deleteHistory(@PathVariable Long agentId) {
        executionHistoryService.deleteHistoryByAgentId(agentId);
        return Result.success("执行历史已清除");
    }
}
