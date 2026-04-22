package com.aiagent.controller;

import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.entity.Role;
import com.aiagent.entity.UserRole;
import com.aiagent.service.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色管理接口")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @RequiresRole("ADMIN")
    public Result<List<Role>> getAllRoles() {
        return Result.success(roleService.getAllRoles());
    }

    @Operation(summary = "获取所有角色列表")
    @GetMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Role> getRoleById(@PathVariable Long id) {
        return Result.success(roleService.getRoleById(id));
    }

    @Operation(summary = "根据ID获取角色详情")
    @PostMapping
    @RequiresRole("ADMIN")
    public Result<Role> createRole(@RequestBody Role role) {
        return Result.success(roleService.createRole(role));
    }

    @Operation(summary = "创建角色")
    @PutMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        return Result.success(roleService.updateRole(id, role));
    }

    @Operation(summary = "更新角色")
    @DeleteMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @PostMapping("/assign")
    @RequiresRole("ADMIN")
    public Result<Void> assignRoleToUser(@Valid @RequestBody AssignRoleRequest request) {
        roleService.assignRoleToUser(request.getUserId(), request.getRoleId());
        return Result.success();
    }

    @Operation(summary = "分配角色给用户")
    @DeleteMapping("/remove")
    @RequiresRole("ADMIN")
    public Result<Void> removeRoleFromUser(@RequestParam Long userId, @RequestParam Long roleId) {
        roleService.removeRoleFromUser(userId, roleId);
        return Result.success();
    }

    @Operation(summary = "从用户移除角色")
    @GetMapping("/user/{userId}")
    @RequiresRole("ADMIN")
    public Result<List<UserRole>> getUserRoles(@PathVariable Long userId) {
        return Result.success(roleService.getUserRoles(userId));
    }

    public static class AssignRoleRequest {
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        @NotNull(message = "角色ID不能为空")
        private Long roleId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }
    }
}
