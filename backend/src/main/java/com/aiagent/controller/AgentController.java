package com.aiagent.controller;

import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.Result;
import com.aiagent.dto.AgentDTO;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent管理", description = "Agent管理接口")
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    @RequiresPermission("agent:view")
    @Operation(summary = "获取所有Agent列表")
    public Result<List<Agent>> getAllAgents() {
        return Result.success(agentService.getAllAgents());
    }

    @GetMapping("/{id}")
    @RequiresPermission("agent:view")
    @Operation(summary = "根据ID获取Agent详情")
    public Result<Agent> getAgentById(@Parameter(description = "Agent ID") @PathVariable Long id) {
        return Result.success(agentService.getAgentById(id));
    }

    @PostMapping
    @RequiresPermission("agent:create")
    @Operation(summary = "创建Agent")
    @OperationLog(value = "创建Agent", module = "Agent管理")
    public Result<Agent> createAgent(@Valid @RequestBody AgentDTO dto) {
        Agent agent = new Agent();
        agent.setName(dto.getName());
        agent.setDescription(dto.getDescription());
        agent.setConfig(dto.getConfig());
        agent.setCategory(dto.getCategory());
        agent.setIsActive(dto.getIsActive());
        if (dto.getType() != null) {
            agent.setStatus(Agent.AgentStatus.valueOf(dto.getType()));
        }
        return Result.success(agentService.createAgent(agent));
    }

    @PutMapping("/{id}")
    @RequiresPermission("agent:update")
    @Operation(summary = "更新Agent")
    @OperationLog(value = "更新Agent", module = "Agent管理")
    public Result<Agent> updateAgent(@Parameter(description = "Agent ID") @PathVariable Long id, @Valid @RequestBody AgentDTO dto) {
        Agent agent = new Agent();
        agent.setName(dto.getName());
        agent.setDescription(dto.getDescription());
        agent.setConfig(dto.getConfig());
        agent.setCategory(dto.getCategory());
        agent.setIsActive(dto.getIsActive());
        if (dto.getType() != null) {
            agent.setStatus(Agent.AgentStatus.valueOf(dto.getType()));
        }
        return Result.success(agentService.updateAgent(id, agent));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("agent:delete")
    @Operation(summary = "删除Agent")
    @OperationLog(value = "删除Agent", module = "Agent管理")
    public Result<Void> deleteAgent(@Parameter(description = "Agent ID") @PathVariable Long id) {
        agentService.deleteAgent(id);
        return Result.success();
    }

    @PostMapping("/{id}/copy")
    @RequiresPermission("agent:create")
    @Operation(summary = "复制Agent")
    public Result<Agent> copyAgent(@Parameter(description = "Agent ID") @PathVariable Long id, @Valid @RequestBody CopyAgentRequest request) {
        return Result.success(agentService.copyAgent(id, request.getNewName()));
    }

    @GetMapping("/{id}/versions")
    @RequiresPermission("agent:view")
    @Operation(summary = "获取Agent版本列表")
    public Result<List<AgentVersion>> getAgentVersions(@Parameter(description = "Agent ID") @PathVariable Long id) {
        return Result.success(agentService.getAgentVersions(id));
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    @RequiresPermission("agent:view")
    @Operation(summary = "获取Agent指定版本详情")
    public Result<AgentVersion> getAgentVersion(@Parameter(description = "Agent ID") @PathVariable Long id, @Parameter(description = "版本号") @PathVariable Integer versionNumber) {
        return Result.success(agentService.getAgentVersion(id, versionNumber));
    }

    @PostMapping("/{id}/versions/{versionNumber}/rollback")
    @RequiresPermission("agent:update")
    @Operation(summary = "回滚到指定版本")
    public Result<Agent> rollbackToVersion(@Parameter(description = "Agent ID") @PathVariable Long id, @Parameter(description = "版本号") @PathVariable Integer versionNumber) {
        return Result.success(agentService.rollbackToVersion(id, versionNumber));
    }

    public static class CopyAgentRequest {
        @NotBlank(message = "名称不能为空")
        @Size(max = 200, message = "名称长度不能超过200个字符")
        private String newName;

        public String getNewName() {
            return newName;
        }

        public void setNewName(String newName) {
            this.newName = newName;
        }
    }
}
