package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.entity.Permission;
import com.aiagent.entity.RolePermission;
import com.aiagent.service.PermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    public Result<List<Permission>> getAllPermissions() {
        return Result.success(permissionService.getAllPermissions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取权限详情")
    @RequiresPermission("permission:read")
    @RequiresRole("ADMIN")
    public Result<Permission> getPermissionById(@PathVariable Long id) {
        return Result.success(permissionService.getPermissionById(id));
    }

    @PostMapping
    @Operation(summary = "创建权限")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        return Result.success(permissionService.createPermission(permission));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新权限")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        return Result.success(permissionService.updatePermission(id, permission));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }

    @PostMapping("/assign")
    @Operation(summary = "分配权限给角色")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<Void> assignPermissionToRole(@Valid @RequestBody AssignPermissionRequest request) {
        permissionService.assignPermissionToRole(request.getRoleId(), request.getPermissionId());
        return Result.success();
    }

    @DeleteMapping("/remove")
    @Operation(summary = "从角色移除权限")
    @RequiresPermission("permission:manage")
    @RequiresRole("ADMIN")
    public Result<Void> removePermissionFromRole(@RequestParam Long roleId, @RequestParam Long permissionId) {
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

    public static class AssignPermissionRequest {
        @NotNull(message = "角色ID不能为空")
        private Long roleId;

        @NotNull(message = "权限ID不能为空")
        private Long permissionId;

        public AssignPermissionRequest() {
        }

        public AssignPermissionRequest(Long roleId, Long permissionId) {
            this.roleId = roleId;
            this.permissionId = permissionId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public Long getPermissionId() {
            return permissionId;
        }

        public void setPermissionId(Long permissionId) {
            this.permissionId = permissionId;
        }
    }
}
