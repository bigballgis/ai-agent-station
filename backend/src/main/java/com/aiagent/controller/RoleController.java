package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.RoleDTO;
import com.aiagent.entity.Role;
import com.aiagent.entity.UserRole;
import com.aiagent.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色管理接口")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "获取所有角色列表")
    @RequiresPermission("role:read")
    @RequiresRole("ADMIN")
    public Result<List<RoleDTO>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        List<RoleDTO> dtoList = roles.stream()
                .map(DTOConverter::toRoleDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取角色详情")
    @RequiresPermission("role:read")
    @RequiresRole("ADMIN")
    public Result<RoleDTO> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        return Result.success(DTOConverter.toRoleDTO(role));
    }

    @PostMapping
    @Operation(summary = "创建角色")
    @RequiresPermission("role:write")
    @RequiresRole("ADMIN")
    public Result<RoleDTO> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        Role role = DTOConverter.toRoleEntity(roleDTO);
        Role created = roleService.createRole(role);
        return Result.success(DTOConverter.toRoleDTO(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色信息")
    @RequiresPermission("role:write")
    @RequiresRole("ADMIN")
    public Result<RoleDTO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO roleDTO) {
        Role role = DTOConverter.toRoleEntity(roleDTO);
        Role updated = roleService.updateRole(id, role);
        return Result.success(DTOConverter.toRoleDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @RequiresPermission("role:manage")
    @RequiresRole("ADMIN")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @PostMapping("/users/{userId}/roles/{roleId}")
    @Operation(summary = "分配角色给用户")
    @RequiresPermission("role:manage")
    @RequiresRole("ADMIN")
    public Result<Void> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        roleService.assignRoleToUser(userId, roleId);
        return Result.success();
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @Operation(summary = "从用户移除角色")
    @RequiresPermission("role:manage")
    @RequiresRole("ADMIN")
    public Result<Void> removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        roleService.removeRoleFromUser(userId, roleId);
        return Result.success();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户角色列表")
    @RequiresPermission("role:read")
    @RequiresRole("ADMIN")
    public Result<List<UserRole>> getUserRoles(@PathVariable Long userId) {
        return Result.success(roleService.getUserRoles(userId));
    }
}
