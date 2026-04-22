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

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @RequiresRole("ADMIN")
    public Result<List<Role>> getAllRoles() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Role> getRoleById(@PathVariable Long id) {
        return Result.success(roleService.getRoleById(id));
    }

    @PostMapping
    @RequiresRole("ADMIN")
    public Result<Role> createRole(@RequestBody Role role) {
        return Result.success(roleService.createRole(role));
    }

    @PutMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        return Result.success(roleService.updateRole(id, role));
    }

    @DeleteMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @PostMapping("/assign")
    @RequiresRole("ADMIN")
    public Result<Void> assignRoleToUser(@Valid @RequestBody AssignRoleRequest request) {
        roleService.assignRoleToUser(request.getUserId(), request.getRoleId());
        return Result.success();
    }

    @DeleteMapping("/remove")
    @RequiresRole("ADMIN")
    public Result<Void> removeRoleFromUser(@RequestParam Long userId, @RequestParam Long roleId) {
        roleService.removeRoleFromUser(userId, roleId);
        return Result.success();
    }

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
