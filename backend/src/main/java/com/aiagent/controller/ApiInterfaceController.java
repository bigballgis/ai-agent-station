package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;

import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.ApiInterfaceCreateDTO;
import com.aiagent.dto.ApiInterfaceUpdateDTO;
import com.aiagent.entity.ApiInterface;
import com.aiagent.service.ApiInterfaceService;
import com.aiagent.vo.ApiInterfaceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/api-interfaces")
@RequiredArgsConstructor
@Tag(name = "API接口管理", description = "API接口管理接口")
public class ApiInterfaceController {

    private final ApiInterfaceService apiInterfaceService;

    @RequiresPermission("api:view")
    @GetMapping
    @Operation(summary = "分页查询API接口列表")
    public Result<PageResult<ApiInterfaceVO>> list(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiInterface> apiPage = apiInterfaceService.listByTenant(tenantId, pageable);
        Page<ApiInterfaceVO> voPage = apiPage.map(ApiInterfaceVO::fromEntity);
        return Result.success(PageResult.from(voPage));
    }

    @RequiresPermission("api:read")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取API接口详情")
    public Result<ApiInterfaceVO> getById(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        return Result.success(ApiInterfaceVO.fromEntity(apiInterfaceService.getById(id, tenantId)));
    }

    @RequiresPermission("api:read")
    @Operation(summary = "根据Agent ID获取API接口列表")
    @GetMapping("/agent/{agentId}")
    public Result<PageResult<ApiInterfaceVO>> listByAgent(
            @PathVariable Long agentId,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        List<ApiInterfaceVO> all = apiInterfaceService.listByAgent(agentId, tenantId).stream()
                .map(ApiInterfaceVO::fromEntity).toList();
        return Result.success(PageResult.paginate(all, page, size));
    }

    @RequiresPermission("api:manage")
    @PostMapping
    @Operation(summary = "创建API接口")
    public Result<ApiInterfaceVO> create(
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @Valid @RequestBody ApiInterfaceCreateDTO dto) {
        ApiInterface apiInterface = new ApiInterface();
        apiInterface.setTenantId(tenantId);
        apiInterface.setAgentId(dto.getAgentId());
        apiInterface.setVersionId(dto.getVersionId());
        apiInterface.setPath(dto.getPath());
        apiInterface.setMethod(dto.getMethod());
        apiInterface.setDescription(dto.getDescription());
        apiInterface.setIsActive(dto.getIsActive());
        apiInterface.setApiVersion(dto.getApiVersion() != null ? dto.getApiVersion() : "v1");
        return Result.success(ApiInterfaceVO.fromEntity(apiInterfaceService.create(apiInterface)));
    }

    @RequiresPermission("api:write")
    @PostMapping("/{id}/version")
    @Operation(summary = "创建API接口新版本")
    public Result<ApiInterfaceVO> createVersion(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @Valid @RequestBody ApiInterfaceUpdateDTO dto) {
        ApiInterface apiInterface = new ApiInterface();
        apiInterface.setAgentId(dto.getAgentId());
        apiInterface.setVersionId(dto.getVersionId());
        apiInterface.setPath(dto.getPath());
        apiInterface.setMethod(dto.getMethod());
        apiInterface.setDescription(dto.getDescription());
        apiInterface.setIsActive(dto.getIsActive());
        return Result.success(ApiInterfaceVO.fromEntity(apiInterfaceService.createNewVersion(id, tenantId, apiInterface)));
    }

    @RequiresPermission("api:read")
    @GetMapping("/{id}/versions")
    @Operation(summary = "获取API接口所有版本")
    public Result<PageResult<ApiInterfaceVO>> listVersions(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        List<ApiInterfaceVO> all = apiInterfaceService.listVersions(id, tenantId).stream()
                .map(ApiInterfaceVO::fromEntity).toList();
        return Result.success(PageResult.paginate(all, page, size));
    }

    @RequiresPermission("api:write")
    @PatchMapping("/{id}/deprecate")
    @Operation(summary = "废弃API接口")
    public Result<ApiInterfaceVO> deprecate(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        return Result.success(ApiInterfaceVO.fromEntity(apiInterfaceService.deprecate(id, tenantId, message)));
    }

    @RequiresPermission("api:write")
    @PutMapping("/{id}")
    @Operation(summary = "更新API接口")
    public Result<ApiInterfaceVO> update(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @Valid @RequestBody ApiInterfaceUpdateDTO dto) {
        ApiInterface apiInterface = new ApiInterface();
        apiInterface.setAgentId(dto.getAgentId());
        apiInterface.setVersionId(dto.getVersionId());
        apiInterface.setPath(dto.getPath());
        apiInterface.setMethod(dto.getMethod());
        apiInterface.setDescription(dto.getDescription());
        apiInterface.setIsActive(dto.getIsActive());
        return Result.success(ApiInterfaceVO.fromEntity(apiInterfaceService.update(id, tenantId, apiInterface)));
    }

    @RequiresPermission("api:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除API接口")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId) {
        apiInterfaceService.delete(id, tenantId);
        return Result.success();
    }

    @RequiresPermission("api:write")
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "切换API接口启用状态")
    public Result<ApiInterfaceVO> toggleActive(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-ID") Long tenantId,
            @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("isActive");
        return Result.success(ApiInterfaceVO.fromEntity(apiInterfaceService.toggleActive(id, tenantId, isActive)));
    }
}
