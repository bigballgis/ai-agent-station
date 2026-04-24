package com.aiagent.controller;

import com.aiagent.annotation.OperationLog;
import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.dto.DTOConverter;
import com.aiagent.dto.CreateUserDTO;
import com.aiagent.dto.UpdateUserDTO;
import com.aiagent.dto.UserDTO;
import com.aiagent.dto.UserResponseDTO;
import com.aiagent.entity.User;
import com.aiagent.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
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
    public Result<PageResult<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码，从0开始") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size) {
        List<UserResponseDTO> allUsers = userService.getAllUsers().stream()
                .map(DTOConverter::toUserResponseDTO)
                .collect(Collectors.toList());
        return Result.success(PageResult.paginate(allUsers, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户详情")
    @RequiresPermission("user:read")
    @RequiresRole("ADMIN")
    public Result<UserResponseDTO> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(DTOConverter.toUserResponseDTO(user));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @RequiresPermission("user:write")
    @RequiresRole("ADMIN")
    @OperationLog(value = "创建用户", module = "用户管理")
    public Result<UserResponseDTO> createUser(@Valid @RequestBody CreateUserDTO dto) {
        User user = DTOConverter.toUserEntity(dto);
        User created = userService.createUser(user);
        return Result.success(DTOConverter.toUserResponseDTO(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息")
    @RequiresPermission("user:write")
    @RequiresRole("ADMIN")
    @OperationLog(value = "更新用户", module = "用户管理")
    public Result<UserResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        User existingUser = userService.getUserById(id);
        DTOConverter.updateUserFromDTO(dto, existingUser);
        User updated = userService.updateUser(id, existingUser);
        return Result.success(DTOConverter.toUserResponseDTO(updated));
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
