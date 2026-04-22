package com.aiagent.controller;

import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.dto.UserDTO;
import com.aiagent.entity.User;
import com.aiagent.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理接口")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "获取所有用户列表")
    @RequiresPermission("user:read")
    @RequiresRole("ADMIN")
    public Result<List<User>> getAllUsers() {
        return Result.success(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户详情")
    @RequiresPermission("user:read")
    @RequiresRole("ADMIN")
    public Result<User> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @RequiresPermission("user:write")
    @RequiresRole("ADMIN")
    @OperationLog(value = "创建用户", module = "用户管理")
    public Result<User> createUser(@Valid @RequestBody UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setTenantId(dto.getTenantId());
        return Result.success(userService.createUser(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息")
    @RequiresPermission("user:write")
    @RequiresRole("ADMIN")
    @OperationLog(value = "更新用户", module = "用户管理")
    public Result<User> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setTenantId(dto.getTenantId());
        return Result.success(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @RequiresPermission("user:delete")
    @RequiresRole("ADMIN")
    @OperationLog(value = "删除用户", module = "用户管理")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码")
    @RequiresPermission("user:manage")
    @RequiresRole("ADMIN")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    public static class ResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 100, message = "密码长度为8-100个字符")
        private String newPassword;

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
