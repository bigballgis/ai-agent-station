package com.aiagent.controller;

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

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @RequiresRole("ADMIN")
    public Result<List<Permission>> getAllPermissions() {
        return Result.success(permissionService.getAllPermissions());
    }

    @GetMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Permission> getPermissionById(@PathVariable Long id) {
        return Result.success(permissionService.getPermissionById(id));
    }

    @PostMapping
    @RequiresRole("ADMIN")
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        return Result.success(permissionService.createPermission(permission));
    }

    @PutMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        return Result.success(permissionService.updatePermission(id, permission));
    }

    @DeleteMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }

    @PostMapping("/assign")
    @RequiresRole("ADMIN")
    public Result<Void> assignPermissionToRole(@Valid @RequestBody AssignPermissionRequest request) {
        permissionService.assignPermissionToRole(request.getRoleId(), request.getPermissionId());
        return Result.success();
    }

    @DeleteMapping("/remove")
    @RequiresRole("ADMIN")
    public Result<Void> removePermissionFromRole(@RequestParam Long roleId, @RequestParam Long permissionId) {
        permissionService.removePermissionFromRole(roleId, permissionId);
        return Result.success();
    }

    @GetMapping("/role/{roleId}")
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
