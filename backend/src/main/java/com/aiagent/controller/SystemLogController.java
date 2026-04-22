package com.aiagent.controller;

import com.aiagent.annotation.RequiresRole;
import com.aiagent.common.PageResult;
import com.aiagent.common.Result;
import com.aiagent.entity.SystemLog;
import com.aiagent.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    @RequiresRole("ADMIN")
    public Result<PageResult<SystemLog>> getLogs(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.success(systemLogService.getLogs(page, size));
    }

    @GetMapping("/date-range")
    @RequiresRole("ADMIN")
    public Result<PageResult<SystemLog>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(systemLogService.getLogsByDateRange(startTime, endTime, page, size));
    }

    @GetMapping("/module/{module}")
    @RequiresRole("ADMIN")
    public Result<PageResult<SystemLog>> getLogsByModule(@PathVariable String module,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(systemLogService.getLogsByModule(module, page, size));
    }

    @GetMapping("/{id}")
    @RequiresRole("ADMIN")
    public Result<SystemLog> getLogById(@PathVariable Long id) {
        return Result.success(systemLogService.getLogById(id));
    }
}
