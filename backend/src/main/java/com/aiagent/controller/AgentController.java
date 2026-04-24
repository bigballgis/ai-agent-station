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
import com.aiagent.exception.BusinessException;
import com.aiagent.service.AgentService;
import com.aiagent.tenant.TenantContextHolder;
import com.aiagent.vo.AgentVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent管理", description = "Agent管理接口")
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    @RequiresPermission("agent:view")
    @Operation(summary = "获取所有Agent列表（分页）", description = "分页查询当前租户下的所有Agent，支持关键词搜索和状态筛选")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<PageResult<AgentVO>> getAllAgents(
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) @Parameter(description = "每页大小") int size,
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
    @Operation(summary = "根据ID获取Agent详情", description = "根据Agent ID获取详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "Agent不存在")
    })
    public Result<AgentVO> getAgentById(@Parameter(description = "Agent ID") @PathVariable Long id) {
        return Result.success(DTOConverter.toAgentVO(agentService.getAgentById(id)));
    }

    @PostMapping
    @RequiresPermission("agent:create")
    @Operation(summary = "创建Agent", description = "创建新的Agent实例")
    @OperationLog(value = "创建Agent", module = "Agent管理")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "409", description = "Agent名称已存在")
    })
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
    @Operation(summary = "更新Agent", description = "根据ID更新Agent信息")
    @OperationLog(value = "更新Agent", module = "Agent管理")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "Agent不存在")
    })
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
    @Operation(summary = "删除Agent", description = "根据ID删除Agent")
    @OperationLog(value = "删除Agent", module = "Agent管理")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "Agent不存在")
    })
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
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) @Parameter(description = "每页大小") int size,
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
            @RequestParam @Min(1) @Max(5) @Parameter(description = "评分(1-5)") int rating) {
        agentService.rateTemplate(id, rating);
        return Result.success();
    }

    // ==================== 导出/导入接口 ====================

    @GetMapping("/export")
    @RequiresPermission("agent:view")
    @Operation(summary = "导出单个Agent为JSON")
    public void exportAgent(
            @RequestParam @Parameter(description = "Agent ID") Long id,
            HttpServletResponse response) throws Exception {
        Agent agent = agentService.getAgentById(id);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("name", agent.getName());
        exportData.put("description", agent.getDescription());
        exportData.put("config", agent.getConfig());
        exportData.put("category", agent.getCategory());
        exportData.put("language", agent.getLanguage());
        exportData.put("tags", agent.getTags());
        exportData.put("isActive", agent.getIsActive());
        exportData.put("status", agent.getStatus().name());

        String filename = agent.getName().replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_");
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "_" + timestamp + ".json\"");
        response.setCharacterEncoding("UTF-8");
        mapper.writerWithDefaultPrettyPrinter().writeValue(response.getOutputStream(), exportData);
    }

    @GetMapping("/export-all")
    @RequiresPermission("agent:view")
    @Operation(summary = "导出所有Agent为JSON数组")
    public void exportAllAgents(HttpServletResponse response) throws Exception {
        List<Agent> agents = agentService.getAllAgents();
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> exportList = agents.stream().map(agent -> {
            Map<String, Object> data = new HashMap<>();
            data.put("name", agent.getName());
            data.put("description", agent.getDescription());
            data.put("config", agent.getConfig());
            data.put("category", agent.getCategory());
            data.put("language", agent.getLanguage());
            data.put("tags", agent.getTags());
            data.put("isActive", agent.getIsActive());
            data.put("status", agent.getStatus().name());
            return data;
        }).collect(Collectors.toList());

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"agents_" + timestamp + ".json\"");
        response.setCharacterEncoding("UTF-8");
        mapper.writerWithDefaultPrettyPrinter().writeValue(response.getOutputStream(), exportList);
    }

    @PostMapping("/import")
    @RequiresPermission("agent:create")
    @Operation(summary = "从JSON导入Agent", description = "导入Agent配置，自动处理名称冲突")
    @OperationLog(value = "导入Agent", module = "Agent管理")
    public Result<AgentVO> importAgent(@RequestBody Map<String, Object> data) {
        // 验证必填字段
        if (data.get("name") == null || data.toString().isEmpty()) {
            throw new BusinessException("导入数据缺少必填字段: name");
        }

        String name = String.valueOf(data.get("name"));
        if (name == null || name.isBlank()) {
            throw new BusinessException("Agent名称不能为空");
        }

        // 处理名称冲突：自动添加后缀
        Long tenantId = TenantContextHolder.getTenantId();
        String originalName = name;
        int suffix = 1;
        while (agentService.existsByNameAndTenantId(name, tenantId)) {
            name = originalName + " (" + suffix + ")";
            suffix++;
        }

        Agent agent = new Agent();
        agent.setName(name);
        agent.setDescription(data.get("description") != null ? String.valueOf(data.get("description")) : null);
        @SuppressWarnings("unchecked")
        Map<String, Object> config = data.get("config") instanceof Map ? (Map<String, Object>) data.get("config") : null;
        agent.setConfig(config);
        agent.setCategory(data.get("category") != null ? String.valueOf(data.get("category")) : null);
        agent.setLanguage(data.get("language") != null ? String.valueOf(data.get("language")) : "zh-CN");
        agent.setIsActive(data.get("isActive") != null ? Boolean.valueOf(data.get("isActive").toString()) : true);
        if (data.get("status") != null) {
            try {
                agent.setStatus(Agent.AgentStatus.valueOf(String.valueOf(data.get("status"))));
            } catch (IllegalArgumentException e) {
                agent.setStatus(Agent.AgentStatus.DRAFT);
            }
        }

        Agent created = agentService.createAgent(agent);
        return Result.success(DTOConverter.toAgentVO(created));
    }

}
