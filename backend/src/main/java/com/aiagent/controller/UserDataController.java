package com.aiagent.controller;

import com.aiagent.common.Result;
import com.aiagent.service.UserDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for GDPR/privacy compliance endpoints.
 * Provides user data export, deletion requests, and data retention reports.
 */
@RestController
@RequestMapping("/v1/user-data")
@RequiredArgsConstructor
@Tag(name = "用户数据管理", description = "GDPR/隐私合规接口")
public class UserDataController {

    private final UserDataService userDataService;

    /**
     * Export current user's data.
     *
     * @param principal the authenticated user principal
     * @return all user data in a structured format
     */
    @GetMapping("/export")
    @Operation(summary = "导出当前用户的所有数据")
    public Result<Map<String, Object>> exportUserData(
            @AuthenticationPrincipal com.aiagent.security.UserPrincipal principal) {
        Map<String, Object> userData = userDataService.exportUserData(principal.getUserId());
        return Result.success(userData);
    }

    /**
     * Request account deletion (anonymization).
     *
     * @param principal the authenticated user principal
     * @return success confirmation
     */
    @PostMapping("/delete-request")
    @Operation(summary = "请求删除账户数据(匿名化处理)")
    public Result<Void> requestAccountDeletion(
            @AuthenticationPrincipal com.aiagent.security.UserPrincipal principal) {
        userDataService.deleteUserData(principal.getUserId());
        return Result.success("账户数据删除请求已提交，数据将在处理完成后被匿名化", null);
    }

    /**
     * Get data retention report (admin only).
     *
     * @return data retention statistics
     */
    @GetMapping("/retention")
    @Operation(summary = "获取数据保留报告(仅管理员)")
    public Result<Map<String, Object>> getDataRetentionReport() {
        Map<String, Object> report = userDataService.getDataRetentionReport();
        return Result.success(report);
    }
}
