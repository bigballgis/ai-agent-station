package com.aiagent.controller;

import com.aiagent.annotation.RequiresPermission;
import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.SystemLog;
import com.aiagent.service.SystemLogService;
import com.aiagent.vo.SystemLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/logs")
@RequiredArgsConstructor
@Tag(name = "系统日志", description = "系统日志管理接口")
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    @Operation(summary = "分页查询系统日志")
    @RequiresPermission("log:read")
    @RequiresRole("ADMIN")
    public Result<PageResult<SystemLogVO>> getLogs(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        PageResult<SystemLog> logPage = systemLogService.getLogs(page, size);
        return Result.success(convertToVoPage(logPage));
    }

    @GetMapping("/date-range")
    @Operation(summary = "按日期范围查询系统日志")
    @RequiresPermission("log:read")
    @RequiresRole("ADMIN")
    public Result<PageResult<SystemLogVO>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<SystemLog> logPage = systemLogService.getLogsByDateRange(startTime, endTime, page, size);
        return Result.success(convertToVoPage(logPage));
    }

    @GetMapping("/module/{module}")
    @Operation(summary = "按模块查询系统日志")
    @RequiresPermission("log:read")
    @RequiresRole("ADMIN")
    public Result<PageResult<SystemLogVO>> getLogsByModule(@PathVariable String module,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        PageResult<SystemLog> logPage = systemLogService.getLogsByModule(module, page, size);
        return Result.success(convertToVoPage(logPage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取日志详情")
    @RequiresPermission("log:read")
    @RequiresRole("ADMIN")
    public Result<SystemLogVO> getLogById(@PathVariable Long id) {
        return Result.success(SystemLogVO.fromEntity(systemLogService.getLogById(id)));
    }

    private PageResult<SystemLogVO> convertToVoPage(PageResult<SystemLog> logPage) {
        return new PageResult<>(
                logPage.getTotal(),
                logPage.getRecords().stream().map(SystemLogVO::fromEntity).toList(),
                logPage.getPage(),
                logPage.getSize()
        );
    }
}
