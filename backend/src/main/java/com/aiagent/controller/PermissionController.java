package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.PermissionDTO;
import com.aiagent.entity.Permission;
import com.aiagent.entity.RolePermission;
import com.aiagent.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "权限管理接口")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @RequiresPermission("permission:read")
    @RequiresRole("ADMIN")
    @Operation(summary = "获取所有权限列表")
    public Result<List<PermissionDTO>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        List<PermissionDTO> dtoList = permissions.stream()
                .map(DTOConverter::toPermissionDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取权限详情")
    @RequiresPermission("permission:read")
    @RequiresRole("ADMIN")
    public Result<PermissionDTO> getPermissionById(@PathVariable Long id) {
        Permission permission = permissionService.getPermissionById(id);
        return Result.success(DTOConverter.toPermissionDTO(permission));
    }

    @PostMapping
    @Operation(summary = "创建权限")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<PermissionDTO> createPermission(@RequestBody PermissionDTO permissionDTO) {
        Permission permission = DTOConverter.toPermissionEntity(permissionDTO);
        Permission created = permissionService.createPermission(permission);
        return Result.success(DTOConverter.toPermissionDTO(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新权限")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<PermissionDTO> updatePermission(@PathVariable Long id, @RequestBody PermissionDTO permissionDTO) {
        Permission permission = DTOConverter.toPermissionEntity(permissionDTO);
        Permission updated = permissionService.updatePermission(id, permission);
        return Result.success(DTOConverter.toPermissionDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }

    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    @Operation(summary = "分配权限给角色")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<Void> assignPermissionToRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        permissionService.assignPermissionToRole(roleId, permissionId);
        return Result.success();
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    @Operation(summary = "从角色移除权限")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<Void> removePermissionFromRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        permissionService.removePermissionFromRole(roleId, permissionId);
        return Result.success();
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "获取角色权限列表")
    @RequiresPermission("permission:read")
    @RequiresRole("ADMIN")
    public Result<List<RolePermission>> getRolePermissions(@PathVariable Long roleId) {
        return Result.success(permissionService.getRolePermissions(roleId));
    }
}
