package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.service.AgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public Result<List<Agent>> getAllAgents() {
        return Result.success(agentService.getAllAgents());
    }

    @GetMapping("/{id}")
    public Result<Agent> getAgentById(@PathVariable Long id) {
        return Result.success(agentService.getAgentById(id));
    }

    @PostMapping
    public Result<Agent> createAgent(@RequestBody Agent agent) {
        return Result.success(agentService.createAgent(agent));
    }

    @PutMapping("/{id}")
    public Result<Agent> updateAgent(@PathVariable Long id, @RequestBody Agent agent) {
        return Result.success(agentService.updateAgent(id, agent));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return Result.success();
    }

    @PostMapping("/{id}/copy")
    public Result<Agent> copyAgent(@PathVariable Long id, @Valid @RequestBody CopyAgentRequest request) {
        return Result.success(agentService.copyAgent(id, request.getNewName()));
    }

    @GetMapping("/{id}/versions")
    public Result<List<AgentVersion>> getAgentVersions(@PathVariable Long id) {
        return Result.success(agentService.getAgentVersions(id));
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    public Result<AgentVersion> getAgentVersion(@PathVariable Long id, @PathVariable Integer versionNumber) {
        return Result.success(agentService.getAgentVersion(id, versionNumber));
    }

    @PostMapping("/{id}/versions/{versionNumber}/rollback")
    public Result<Agent> rollbackToVersion(@PathVariable Long id, @PathVariable Integer versionNumber) {
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
