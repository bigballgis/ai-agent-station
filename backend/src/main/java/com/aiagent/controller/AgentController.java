package com.aiagent.controller;

import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;
import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.AgentCopyDTO;
import com.aiagent.dto.AgentDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.service.AgentService;
import com.aiagent.tenant.TenantContextHolder;
import com.aiagent.vo.AgentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent管理", description = "Agent管理接口")
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    @RequiresPermission("agent:view")
    @Operation(summary = "获取所有Agent列表（分页）")
    public Result<PageResult<AgentVO>> getAllAgents(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword,
            @RequestParam(required = false) @Parameter(description = "状态筛选") String status) {
        Long tenantId = TenantContextHolder.getTenantId();
        Page<Agent> agentPage = agentService.getAgentsPaged(tenantId, keyword, status, page, size);
        List<AgentVO> voList = agentPage.getContent().stream()
                .map(DTOConverter::toAgentVO)
                .collect(Collectors.toList());
        return Result.success(PageResult.from(agentPage.map(DTOConverter::toAgentVO)));
    }

    @GetMapping("/{id}")
    @RequiresPermission("agent:view")
    @Operation(summary = "根据ID获取Agent详情")
    public Result<AgentVO> getAgentById(@Parameter(description = "Agent ID") @PathVariable Long id) {
        return Result.success(DTOConverter.toAgentVO(agentService.getAgentById(id)));
    }

    @PostMapping
    @RequiresPermission("agent:create")
    @Operation(summary = "创建Agent")
    @OperationLog(value = "创建Agent", module = "Agent管理")
    public Result<AgentVO> createAgent(@Valid @RequestBody AgentDTO dto) {
        Agent agent = new Agent();
        agent.setName(dto.getName());
        agent.setDescription(dto.getDescription());
        agent.setConfig(dto.getConfig());
        agent.setCategory(dto.getCategory());
        agent.setIsActive(dto.getIsActive());
        agent.setIsTemplate(dto.getIsTemplate());
        if (dto.getType() != null) {
            agent.setStatus(Agent.AgentStatus.valueOf(dto.getType()));
        }
        return Result.success(DTOConverter.toAgentVO(agentService.createAgent(agent)));
    }

    @PutMapping("/{id}")
    @RequiresPermission("agent:update")
    @Operation(summary = "更新Agent")
    @OperationLog(value = "更新Agent", module = "Agent管理")
    public Result<AgentVO> updateAgent(@Parameter(description = "Agent ID") @PathVariable Long id, @Valid @RequestBody AgentDTO dto) {
        Agent agent = new Agent();
        agent.setName(dto.getName());
        agent.setDescription(dto.getDescription());
        agent.setConfig(dto.getConfig());
        agent.setCategory(dto.getCategory());
        agent.setIsActive(dto.getIsActive());
        if (dto.getType() != null) {
            agent.setStatus(Agent.AgentStatus.valueOf(dto.getType()));
        }
        return Result.success(DTOConverter.toAgentVO(agentService.updateAgent(id, agent)));
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
    public Result<AgentVO> copyAgent(@Parameter(description = "Agent ID") @PathVariable Long id, @Valid @RequestBody AgentCopyDTO request) {
        return Result.success(DTOConverter.toAgentVO(agentService.copyAgent(id, request.getNewName())));
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
    public Result<AgentVO> rollbackToVersion(@Parameter(description = "Agent ID") @PathVariable Long id, @Parameter(description = "版本号") @PathVariable Integer versionNumber) {
        return Result.success(DTOConverter.toAgentVO(agentService.rollbackToVersion(id, versionNumber)));
    }

    // ==================== 模板市场接口 ====================

    @GetMapping("/templates")
    @Operation(summary = "获取模板列表", description = "分页查询模板，支持关键词搜索和分类筛选")
    public Result<PageResult<AgentVO>> getTemplates(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword,
            @RequestParam(required = false) @Parameter(description = "分类筛选") String category) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "usageCount"));
        Page<Agent> templatePage = agentService.getTemplatesPaged(keyword, category, pageable);
        return Result.success(PageResult.from(templatePage.map(DTOConverter::toAgentVO)));
    }

    @PostMapping("/templates/{id}/use")
    @RequiresPermission("agent:create")
    @Operation(summary = "使用模板创建Agent", description = "基于模板创建新的Agent实例")
    public Result<AgentVO> useTemplate(@Parameter(description = "模板 ID") @PathVariable Long id) {
        return Result.success(DTOConverter.toAgentVO(agentService.createFromTemplate(id)));
    }

    @PostMapping("/templates/{id}/rate")
    @Operation(summary = "为模板评分", description = "对模板进行1-5星评分")
    public Result<Void> rateTemplate(
            @Parameter(description = "模板 ID") @PathVariable Long id,
            @RequestParam @Parameter(description = "评分(1-5)") int rating) {
        agentService.rateTemplate(id, rating);
        return Result.success();
    }

}
