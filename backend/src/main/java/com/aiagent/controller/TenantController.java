package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.dto.CreateTenantRequestDTO;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.UpdateTenantRequestDTO;
import com.aiagent.entity.Tenant;
import com.aiagent.service.TenantService;
import com.aiagent.vo.TenantVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "租户管理", description = "租户管理接口")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @Operation(summary = "获取所有租户列表")
    @RequiresPermission("tenant:read")
    @RequiresRole("SUPER_ADMIN")
    public Result<List<TenantVO>> getAllTenants() {
        return Result.success(tenantService.getAllTenants().stream().map(DTOConverter::toTenantVO).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取租户详情")
    @RequiresPermission("tenant:read")
    @RequiresRole("SUPER_ADMIN")
    public Result<TenantVO> getTenantById(@PathVariable Long id) {
        return Result.success(DTOConverter.toTenantVO(tenantService.getTenantById(id)));
    }

    @PostMapping
    @Operation(summary = "创建租户")
    @RequiresPermission("tenant:write")
    @RequiresRole("SUPER_ADMIN")
    public Result<TenantVO> createTenant(@Valid @RequestBody CreateTenantRequestDTO requestDTO) {
        Tenant tenant = DTOConverter.toTenantEntity(requestDTO);
        return Result.success(DTOConverter.toTenantVO(tenantService.createTenant(tenant)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新租户信息")
    @RequiresPermission("tenant:write")
    @RequiresRole("SUPER_ADMIN")
    public Result<TenantVO> updateTenant(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequestDTO requestDTO) {
        Tenant existing = tenantService.getTenantById(id);
        DTOConverter.updateTenantFromDTO(requestDTO, existing);
        return Result.success(DTOConverter.toTenantVO(tenantService.updateTenant(id, existing)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户")
    @RequiresPermission("tenant:manage")
    @RequiresRole("SUPER_ADMIN")
    public Result<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return Result.success();
    }

    @PostMapping("/{id}/regenerate-api-key")
    @Operation(summary = "重新生成API密钥")
    @RequiresPermission("tenant:manage")
    @RequiresRole("SUPER_ADMIN")
    public Result<TenantVO> regenerateApiKey(@PathVariable Long id) {
        return Result.success(DTOConverter.toTenantVO(tenantService.regenerateApiKey(id)));
    }
}
