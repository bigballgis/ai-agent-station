package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.Result;
import com.aiagent.dto.ExecutionHistoryResponseDTO;
import com.aiagent.entity.ExecutionHistory;
import com.aiagent.repository.ExecutionHistoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/execution-history")
@RequiredArgsConstructor
@Tag(name = "执行历史", description = "SSE执行历史管理接口")
public class ExecutionHistoryController {

    private final ExecutionHistoryRepository executionHistoryRepository;

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "获取Agent执行历史")
    @RequiresPermission("agent:read")
    public Result<List<ExecutionHistoryResponseDTO>> getHistory(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<ExecutionHistory> historyPage = executionHistoryRepository
                .findByAgentIdOrderByTimestampDesc(agentId, PageRequest.of(page, size));
        List<ExecutionHistoryResponseDTO> dtoList = historyPage.getContent().stream().map(this::toDTO).toList();
        return Result.success(dtoList);
    }

    @DeleteMapping("/agent/{agentId}")
    @Operation(summary = "清除Agent执行历史")
    @RequiresPermission("agent:read")
    public Result<?> deleteHistory(@PathVariable Long agentId) {
        executionHistoryRepository.deleteByAgentId(agentId);
        return Result.success("执行历史已清除");
    }

    private ExecutionHistoryResponseDTO toDTO(ExecutionHistory entity) {
        ExecutionHistoryResponseDTO dto = new ExecutionHistoryResponseDTO();
        dto.setId(entity.getId());
        dto.setAgentId(entity.getAgentId());
        dto.setTenantId(entity.getTenantId());
        dto.setUserId(entity.getUserId());
        dto.setMessage(entity.getMessage());
        dto.setRole(entity.getRole());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }
}
