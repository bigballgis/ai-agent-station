package com.aiagent.controller;

import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.Result;
import com.aiagent.entity.User;
import com.aiagent.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequiresRole("ADMIN")
    public Result<List<User>> getAllUsers() {
        return Result.success(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<User> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PostMapping
    @RequiresRole("ADMIN")
    public Result<User> createUser(@RequestBody User user) {
        return Result.success(userService.createUser(user));
    }

    @PutMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return Result.success(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PostMapping("/{id}/reset-password")
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
